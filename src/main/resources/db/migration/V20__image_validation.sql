-- =============================================================================
-- V20 — Fingerprint image structural validation + runtime platform settings.
--
-- platform_settings: small key/value store for admin-editable runtime settings
-- (JSON per key). First key: IMAGE_VALIDATION (format / size / resolution /
-- blank-image rules applied to biometrics.image before the backend call).
--
-- transactions.image_*: what the platform saw in the fingerprint image
-- (format, dimensions, declared resolution, size) and the check outcome.
-- The image itself is never stored.
-- =============================================================================

create table platform_settings (
    setting_key  varchar(64)   not null primary key,
    value_json   text          not null,
    updated_by   varchar(255)  null,
    updated_at   timestamp(6)  not null
);

alter table transactions
    add column image_format        varchar(8)   null,
    add column image_width         int          null,
    add column image_height        int          null,
    add column image_ppi           int          null,
    add column image_bytes         int          null,
    add column image_check         varchar(8)   null,
    add column image_check_message varchar(255) null;
