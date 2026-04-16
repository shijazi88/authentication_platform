# Deploying Sannad to Railway

Three Railway services:

| Service | What | Image |
|---|---|---|
| **`mysql`** | Database | Railway MySQL plugin (managed) |
| **`backend`** | Spring Boot API | Built from repo root `Dockerfile` |
| **`portal`** | React admin UI | Built from `portal-admin/Dockerfile` |

---

## Prerequisites

1. GitHub account with the repo pushed (see "Push to GitHub" below)
2. Railway account — [railway.app](https://railway.app)
3. Railway CLI (optional but useful): `brew install railway`

---

## Step 0 — Push code to GitHub

From your local machine:

```bash
cd "/Users/rushai/Documents/work/authentication middleware"

git init
git branch -M main

# Stage everything except what .gitignore excludes
git add -A
git commit -m "Initial commit: Sannad identity verification platform"

git remote add origin https://github.com/shijazi88/authentication_platform.git
git push -u origin main
```

If `git push` asks for credentials, use a **GitHub Personal Access Token** as the password (not your account password). Create one at https://github.com/settings/tokens with `repo` scope.

---

## Step 1 — Create Railway project and add MySQL

1. Open https://railway.app/new and click **"Deploy from GitHub repo"**.
2. Authorize Railway to read your GitHub, pick `shijazi88/authentication_platform`.
3. Railway will try to auto-detect a service. Cancel the auto-deploy for now — we want to configure it manually.
4. In the project dashboard, click **"+ New"** → **"Database"** → **"Add MySQL"**. Railway provisions a MySQL 8 instance and sets its own internal networking.

Railway gives the MySQL plugin these env vars automatically:
- `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`
- `MYSQL_URL` (JDBC-style)

We'll reference those from the backend service next.

---

## Step 2 — Deploy the backend service

1. In the project, click **"+ New"** → **"GitHub Repo"** → pick the same repo again. This creates a second service.
2. Open the new service's **Settings** tab:
   - **Service Name** → `backend`
   - **Root Directory** → leave empty (repo root)
   - **Builder** → Dockerfile (auto-detected from the repo-root `Dockerfile` and `railway.json`)

3. Open the **Variables** tab and add these (click "Reference Variable" for the MySQL ones so they link to the plugin):

| Variable | Value | Source |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | plain |
| `DB_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true` | reference |
| `DB_USERNAME` | `${{MySQL.MYSQLUSER}}` | reference |
| `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` | reference |
| `PLATFORM_SECURITY_JWT_SECRET` | Any random string **≥ 32 bytes**. Generate: `openssl rand -hex 32` | plain |
| `PLATFORM_SECURITY_JWT_ISSUER` | `sannad-platform` (or your own) | plain |
| `YEMEN_ID_BASE_URL` | The real Yemen ID API base URL, or your WireMock URL for testing | plain |
| `YEMEN_ID_BEARER_TOKEN` | The provider's bearer token, or `dev-token` for WireMock | plain |
| `CORS_ALLOWED_ORIGINS` | `https://<portal-service>.up.railway.app` (fill in after step 3) | plain |
| `BOOTSTRAP_ADMIN_ENABLED` | `true` (only for the very first deploy, then delete it) | plain |
| `BOOTSTRAP_ADMIN_EMAIL` | your admin email | plain |
| `BOOTSTRAP_ADMIN_PASSWORD` | strong password, change after first login | plain |

4. Open the **Settings → Networking** tab, click **Generate Domain**. Railway assigns `https://<backend-service>.up.railway.app`.

5. Click **Deploy**. Railway builds the Dockerfile, pushes it, and starts the container. First build ≈ 3–5 minutes (Maven downloading deps, extracting layers).

6. Watch the **Deployments → Logs** tab. You should see:
   ```
   Migrating schema `railway` to version "1 - core iam"
   …
   Successfully applied 8 migrations to schema `railway`, now at version v8
   Started PlatformApplication in 3.xx seconds
   ```

7. Test the health check:
   ```bash
   curl https://<backend-service>.up.railway.app/actuator/health
   # {"status":"UP"}
   ```

---

## Step 3 — Deploy the portal service

1. In the project, click **"+ New"** → **"GitHub Repo"** → pick the same repo a third time.
2. Open the new service's **Settings** tab:
   - **Service Name** → `portal`
   - **Root Directory** → `portal-admin`
   - **Builder** → Dockerfile (auto-detected from `portal-admin/Dockerfile`)

3. Open the **Variables** tab:

| Variable | Value |
|---|---|
| `VITE_API_URL` | `https://<backend-service>.up.railway.app` (from step 2.4) |

The `VITE_API_URL` is baked into the JS bundle at **build time**. If you change it later, you must redeploy.

4. Open **Settings → Networking**, click **Generate Domain**. Railway assigns `https://<portal-service>.up.railway.app`.

5. Go back to the **backend** service's **Variables** tab and update `CORS_ALLOWED_ORIGINS` to the portal domain from step 3.4. Redeploy the backend (Railway does this automatically when env vars change).

6. Click **Deploy** on the portal. First build ≈ 1–2 minutes.

---

## Step 4 — Log in

1. Open `https://<portal-service>.up.railway.app`
2. Sign in with the bootstrap admin credentials you set in step 2.3.
3. **Change the password** immediately via the admin settings (or issue a new admin user and disable bootstrap).
4. Go back to the backend **Variables** tab and **delete** (or set to `false`) the `BOOTSTRAP_ADMIN_*` variables. You don't want them sitting in prod config.

---

## Step 5 — Onboard the first bank

Same flow as locally:

1. Admin portal → **Tenants → New tenant** → `code=BANK_X, legalName=…`
2. Activate the tenant.
3. **Issue credential** — copy the `clientSecret` immediately, you cannot retrieve it later.
4. **Subscriptions → New subscription** → pick the tenant + plan (seeded `YEMEN_ID_BASIC` or `YEMEN_ID_PREMIUM`).
5. Share the `clientId` + `clientSecret` with the bank + the [Postman collection](../postman/).

---

## Railway-specific gotchas

### The `PORT` env var
Railway injects `PORT` at runtime. Both Dockerfiles respect it:
- Backend: `application.yml` sets `server.port: ${PORT:8080}` and the Dockerfile passes it through.
- Frontend: nginx uses `envsubst` to rewrite `listen ${PORT}` at container start.

### Private networking
Services in the same Railway project can reach each other on `<service-name>.railway.internal` — but in our setup, the browser is the client, so we need the **public** domain in `VITE_API_URL`.

### Database SSL
Railway MySQL doesn't require SSL for intra-project traffic. The JDBC URL above sets `useSSL=false`. If you later move to a self-hosted DB that requires SSL, change the connection string.

### Cold starts
Railway's free tier spins containers down after inactivity. The first request after a cold start can take 10–30 seconds while Spring Boot warms up. Paid tiers keep containers warm.

### Cost estimate
- MySQL plugin: ~$5/mo (shared), ~$10/mo (dedicated)
- Backend: ~$5–10/mo (512 MB RAM is enough for small traffic)
- Frontend: ~$1/mo (nginx + static files is very cheap)
- **Total: ~$11–21/mo** for a production-grade pilot

---

## Railway CLI shortcuts (optional)

If you install the Railway CLI, you can:

```bash
# Link your local repo to the project
railway link

# Pull env vars down for local testing
railway run ./mvnw spring-boot:run

# Deploy from CLI
railway up

# Tail logs
railway logs
```

---

## Rolling out updates

Once configured, every `git push` to `main` triggers:
- Backend rebuilds + redeploys (Flyway auto-applies any new migrations)
- Portal rebuilds + redeploys

No manual steps. If a build fails, the previous deployment keeps serving — zero downtime on healthy deploys.

---

## Environment variable reference (full list)

### Backend service required in prod

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | Activates `application-prod.yml` (refuses to start without secrets) |
| `DB_URL` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | DB credentials |
| `PLATFORM_SECURITY_JWT_SECRET` | ≥ 32 bytes HMAC secret |
| `YEMEN_ID_BASE_URL` / `YEMEN_ID_BEARER_TOKEN` | Upstream Yemen ID provider |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of portal origins |

### Backend service optional

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | 8080 | Port Railway injects |
| `PLATFORM_SECURITY_JWT_ISSUER` | `middleware-platform` | JWT `iss` claim |
| `BOOTSTRAP_ADMIN_ENABLED` | `false` | Seed admin on first boot |
| `BOOTSTRAP_ADMIN_EMAIL` | | Bootstrap admin email |
| `BOOTSTRAP_ADMIN_PASSWORD` | | Bootstrap admin password |

### Portal service (build-time)

| Variable | Purpose |
|---|---|
| `VITE_API_URL` | Backend public URL; baked into the JS bundle at build time |
