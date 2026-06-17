-- =============================================================================
-- V14 — Per-tenant encryption keypairs for PII payload encryption (JWE).
--
-- Each tenant has at least one ACTIVE keypair. Clients encrypt the verification
-- PII to the tenant's public cert (RSA-OAEP-256 + A256GCM); the gateway decrypts
-- with the private key. Private keys are stored PEM-encrypted at rest via the
-- application's column encryption (EncryptedStringConverter).
--
-- Rotation: a new ACTIVE key is added, the previous one moves to RETIRING (still
-- decrypts in-flight requests), then REVOKED.
-- =============================================================================

create table tenant_encryption_keys (
    id                         varchar(36)  not null,
    tenant_id                  varchar(36)  not null,
    kid                        varchar(64)  not null,
    algorithm                  varchar(32)  not null default 'RSA-OAEP-256',
    public_cert_pem            mediumtext   not null,
    private_key_pem_encrypted  mediumtext   not null,
    status                     varchar(16)  not null,         -- ACTIVE | RETIRING | REVOKED
    created_at                 datetime(6)  not null,
    rotated_at                 datetime(6),
    expires_at                 datetime(6),
    primary key (id),
    unique key uk_tenant_keys_kid (kid),
    key idx_tenant_keys_tenant_status (tenant_id, status),
    constraint fk_tenant_keys_tenant foreign key (tenant_id) references tenants (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
