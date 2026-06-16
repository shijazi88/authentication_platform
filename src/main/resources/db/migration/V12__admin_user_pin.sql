-- =============================================================================
-- V12 — Per-admin PIN for step-up access to sensitive pages (Transactions).
--
-- Nullable: a user has no PIN until they set one. Stored BCrypt-hashed, never
-- in clear text. Verifying the PIN issues a short-lived unlock token.
-- =============================================================================

alter table admin_users
    add column pin_hash varchar(255) null after password_hash;
