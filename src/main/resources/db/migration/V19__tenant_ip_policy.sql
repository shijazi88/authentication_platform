-- =============================================================================
-- V19 — Tenant-level IP access policy for the bank-facing API.
--
-- ip_policy:    ALL        → accept calls from any source IP (default)
--               RESTRICTED → accept only from ip_allowlist
-- ip_allowlist: comma-separated IPv4 addresses / CIDR ranges (RESTRICTED only).
--
-- Enforced in ClientCredentialsAuthFilter on top of the per-credential
-- api_credentials.ip_allowlist, so an admin can lock a whole bank to its
-- egress IPs without re-issuing keys.
-- =============================================================================

alter table tenants
    add column ip_policy    varchar(16)   not null default 'ALL',
    add column ip_allowlist varchar(2048) null;
