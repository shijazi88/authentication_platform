-- =============================================================================
-- V15 — Per-tenant enforcement of encrypted verification PII.
--
-- When true, the gateway rejects legacy plaintext verify requests for this
-- tenant (only the encrypted JWE envelope is accepted). Lets enforcement be
-- rolled out tenant-by-tenant once each bank's capture app is upgraded. A
-- global override (platform.crypto.require-encrypted-pii) still applies on top.
-- =============================================================================

alter table tenants
    add column require_encrypted_pii boolean not null default false;
