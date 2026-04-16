# Authentication Middleware Platform

A multi-tenant verification middleware that brokers requests between **banks** (tenants /
service providers) and **government backend verification services** (Yemen ID, etc.).

It provides:
- Unified REST API for banks (`POST /api/v1/verify/identity`) wrapping Yemen ID `/v1/id/verify`.
- Tenant onboarding + per-tenant API credentials.
- Plan-based subscriptions with **operation-level access control** and
  **field-level response masking** (the "hit-only vs hit-with-data" toggle).
- Per-transaction billing event capture, ready for monthly invoice aggregation.
- Pluggable connector SPI for adding new backend providers.
- Full transaction store with audit trail and provider payload archival.

## Stack

- Java 21 source level (compiles & runs on JDK 21+, including JDK 25)
- Spring Boot 3.4.x
- **MySQL 8** (Flyway migrations)
- Spring Security (HTTP Basic for tenants, JWT for admin portal)
- Spring WebFlux WebClient (backend connectors)
- Resilience4j (circuit breaker, retry on Yemen ID)
- Lombok, JJWT, springdoc-openapi
- Modular monolith (single deployable, package-bounded contexts) — easy to extract
  modules into microservices later.

## Toolchain

| Tool | Required | Notes |
|---|---|---|
| **JDK 21+** | Yes | JDK 25 is supported. Set `JAVA_HOME` (see below). |
| **Maven** | No (bundled) | Use `./mvnw` — the Maven Wrapper bootstraps Maven 3.9.9 on first run. |
| **Gradle** | No | Project is Maven-based. |
| **Docker** | Yes (for local dev) | Used to run MySQL and WireMock locally. |
| **MySQL CLI** | Optional | Only needed if you want to inspect the DB outside Docker. |

### Set JAVA_HOME (one-time, zsh)

```bash
echo 'export JAVA_HOME="$(/usr/libexec/java_home)"' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version   # should print 25.x.x
```

## Module map

```
src/main/java/com/middleware/platform/
├── PlatformApplication.java     // entrypoint
├── common/                      // errors, tenant context, request id, utilities
├── iam/                         // tenants, credentials, admin users, security filters
├── catalog/                     // backend service & operation registry, field dictionary
├── subscription/                // plans, plan entitlements, field entitlements, resolver
├── connector/
│   ├── spi/                     // VerificationConnector SPI + registry
│   └── yemenid/                 // Yemen ID v0.1.0 connector implementation
├── gateway/                     // verification orchestrator, field projector, /api/v1/...
├── transactions/                // hot+cold transaction store, domain events
├── billing/                     // billing event capture (listens to transaction events)
└── config/                      // WebClient + cross-cutting beans
```

## Configuration profiles

Four Spring profiles are pre-wired:

| Profile | When to use | Key behavior |
|---|---|---|
| `dev` (default) | Local laptop development | MySQL on `localhost:3306` (root / `12345678`), WireMock backend, bootstrap admin enabled, debug logging |
| `test` | `./mvnw test` runs | Separate `yemen_test` schema, smaller pool, env-overridable |
| `staging` | Pre-production | All secrets from env vars, bootstrap admin off, INFO logging, error messages hidden |
| `prod` | Production | Strictest secrets, larger pools, errors and stack traces hidden, actuator restricted |

### How to activate a profile

```bash
# 1. Default — uses application.yml's default (dev)
./mvnw spring-boot:run

# 2. CLI argument
./mvnw spring-boot:run -Dspring-boot.run.profiles=staging

# 3. Environment variable
SPRING_PROFILES_ACTIVE=prod java -jar target/authentication-middleware-0.1.0-SNAPSHOT.jar

# 4. JVM system property
java -Dspring.profiles.active=prod -jar target/authentication-middleware-0.1.0-SNAPSHOT.jar
```

### Required environment variables for staging / prod

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:mysql://db.internal:3306/yemen?useSSL=true&serverTimezone=UTC` |
| `DB_USERNAME` | DB username |
| `DB_PASSWORD` | DB password |
| `PLATFORM_SECURITY_JWT_SECRET` | ≥32-byte HMAC secret for admin JWTs |
| `YEMEN_ID_BASE_URL` | Real Yemen ID provider base URL |
| `YEMEN_ID_BEARER_TOKEN` | Bearer token from the provider |
| `BOOTSTRAP_ADMIN_ENABLED` (staging only) | `true` for first deploy, then unset |

The app refuses to start in staging/prod if any of the required vars are missing —
the placeholder syntax `${VAR:?message}` makes that explicit.

## Running locally (dev profile)

### 1. Start dependencies
```bash
docker compose up -d mysql wiremock
```
- **MySQL 8** on `localhost:3306` — db `yemen`, user `root`, password `12345678`.
  Schema is auto-created via `createDatabaseIfNotExist=true` in the JDBC URL.
- **WireMock** on `localhost:8089` simulating Yemen ID at `/base_path/v1/id/verify`
  (stub at `wiremock/mappings/yemen-id-verify.json`)

Verify MySQL is reachable:
```bash
docker exec -it yemen-mysql mysql -uroot -p12345678 -e "show databases;"
```

### 2. Run the application
```bash
./mvnw spring-boot:run
```
First run downloads Maven 3.9.9 into `~/.m2/wrapper`. Subsequent runs are fast.
Or import into your IDE and run `PlatformApplication` with profile `dev`.

On first start the platform will:
- Run all Flyway migrations (creates tables, seeds the Yemen ID service catalog and
  two reference plans: `YEMEN_ID_BASIC` (hit-only) and `YEMEN_ID_PREMIUM` (full data))
- Seed a bootstrap admin user (`admin@middleware.local` / `admin123`)
  — **change immediately in any non-dev environment**.

### 3. Try it end-to-end

#### a) Log in as admin
```bash
curl -s -X POST http://localhost:8080/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@middleware.local","password":"admin123"}'
```
Returns `{ "accessToken": "...", "tokenType": "Bearer", ... }`. Export it:
```bash
TOKEN=...   # paste accessToken
```

#### b) Create a tenant (bank)
```bash
curl -s -X POST http://localhost:8080/admin/tenants \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"code":"BANK_DEMO","legalName":"Demo Bank Ltd","contactEmail":"ops@demo.bank"}'
```
Note the `id` returned (`TENANT_ID`).

#### c) Activate the tenant
```bash
curl -s -X POST "http://localhost:8080/admin/tenants/$TENANT_ID/status?status=ACTIVE" \
  -H "Authorization: Bearer $TOKEN"
```

#### d) Issue API credentials for the bank
```bash
curl -s -X POST "http://localhost:8080/admin/tenants/$TENANT_ID/credentials" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"label":"Production"}'
```
Returns `{ "clientId":"cli_...", "clientSecret":"sec_..." }` — **the secret is shown only once**.

#### e) Subscribe the tenant to a plan
List plans:
```bash
curl -s http://localhost:8080/admin/plans -H "Authorization: Bearer $TOKEN"
```
Pick the BASIC plan (`40000000-0000-0000-0000-000000000001`) for hit-only,
or PREMIUM (`40000000-0000-0000-0000-000000000002`) for full data:
```bash
curl -s -X POST http://localhost:8080/admin/subscriptions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"tenantId\":\"$TENANT_ID\",\"planId\":\"40000000-0000-0000-0000-000000000001\",\"startDate\":\"2026-01-01\"}"
```

#### f) Bank calls the verification API
Using HTTP Basic auth with the issued credentials:
```bash
curl -s -X POST http://localhost:8080/api/v1/verify/identity \
  -u "$CLIENT_ID:$CLIENT_SECRET" \
  -H 'Content-Type: application/json' \
  -d '{"nationalNumber":"B123ADSBADADASDFCFDD"}'
```

**With BASIC plan (hit-only)** the response will contain only:
```json
{
  "transaction": { "id": "...", "timestamp": "...", "status": "OK" },
  "result": {
    "transaction": { "id": "...", "timestamp": "..." },
    "verification": { "status": "MATCH", "biometric": { "status": true, "score": 92 } }
  }
}
```
**With PREMIUM plan (full data)** the response also includes `result.person.demographics`,
`result.person.cards`, etc.

The mask is applied by `FieldProjector` based on `plan_field_entitlements` — switching
plans changes the response **with no code or connector changes**.

#### g) Inspect transactions and billing
```bash
curl -s "http://localhost:8080/admin/transactions?tenantId=$TENANT_ID" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/admin/billing/summary?tenantId=$TENANT_ID&period=2026-04" \
  -H "Authorization: Bearer $TOKEN"
```

## Key concepts

### Entitlement-driven response masking
The masking feature is wired up via two tables:
- `field_definitions` — every dot-path the gateway can return for a given operation.
- `plan_field_entitlements` — which paths each plan unlocks.

`FieldProjector.project(canonicalResponse, allowedPaths)` walks the response tree and
keeps only the paths whitelisted by the tenant's active subscription. Adding a new
plan tier = inserting rows; no Java changes.

### Connector SPI
Adding a new backend provider:
1. Implement `VerificationConnector` (key + invoke) and annotate as `@Component`.
2. Insert a `service_definitions` row whose `connector_key` matches your `key()`.
3. Insert one or more `service_operations` rows.
4. Insert `field_definitions` for everything your connector can return.
5. (Optional) extend `gateway/api/VerifyController` with a new path that calls
   `orchestrator.execute(YOUR_KEY, OPERATION, payload)`.

### Transactions and billing
`TransactionService` writes the row, archives the payloads, and publishes a
`TransactionCompletedEvent`. The `TransactionListener` in the billing module catches
that event **after the originating transaction commits** (`@TransactionalEventListener`,
`AFTER_COMMIT`), so a billing failure can never roll back a verification.

For v1 the event bus is in-process Spring `ApplicationEventPublisher`. To go
distributed, replace `publishEvent` with a Kafka producer + outbox table — no other
producer changes needed.

## Hardening checklist before production

- [ ] Switch profile to `prod` (`SPRING_PROFILES_ACTIVE=prod`).
- [ ] Set `PLATFORM_SECURITY_JWT_SECRET` to a random 256-bit key sourced from
      Vault / AWS Secrets Manager (the prod profile refuses to start without it).
- [ ] Confirm `platform.security.bootstrap-admin.enabled=false` (the default in
      `application-prod.yml`) and provision real admin users via a sealed `INSERT`
      migration.
- [ ] Encrypt `transaction_payloads` columns at rest (KMS data keys, AES-GCM).
      Especially relevant for biometric `image` payloads.
- [ ] Wire IP allowlist enforcement in `ClientCredentialsAuthFilter`
      (`api_credentials.ip_allowlist` already exists in the schema).
- [ ] Add per-tenant rate limiting (Redis Bucket4j) — quota fields are already in
      `plan_entitlements`.
- [ ] Replace in-process events with Kafka + outbox.
- [ ] Add Testcontainers integration tests.
- [ ] Wire OpenTelemetry exporter to your observability backend.
- [ ] Generate monthly invoices from `billing_events` (cron + PDF/CSV export).

## API documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` once the app is running.
