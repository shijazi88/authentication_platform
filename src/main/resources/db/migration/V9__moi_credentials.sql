-- ============================================================================
-- V9: MOI (Yemen Ministry of Interior) API credentials — single-row config
-- table storing the login + verify URLs and credentials used by the live
-- MoiYemenIdConnector. The password is stored via EncryptedStringConverter
-- at the application layer (AES-GCM). One row only; enforced via PK=1.
-- ============================================================================

create table moi_credentials (
    id                      bigint      not null,
    auth_url                varchar(512) not null,
    verify_url              varchar(512) not null,
    username                varchar(255) not null,
    password_encrypted      varchar(2048) not null,
    domain_name             varchar(128) not null,
    active                  boolean      not null default false,
    connect_timeout_ms      int          not null default 10000,
    read_timeout_ms         int          not null default 30000,
    token_refresh_skew_sec  int          not null default 60,
    updated_by              varchar(255),
    updated_at              datetime(6)  not null,
    created_at              datetime(6)  not null,
    primary key (id),
    constraint ck_moi_credentials_single check (id = 1)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- Seed the single row with the values from MOI's integration doc.
-- Password is seeded as plain — first app boot will re-save it encrypted if
-- the encryption key is configured. This is intentional so ops can boot
-- without hand-editing DB hashes; rotate via PUT /admin/moi-credentials.
insert into moi_credentials (
    id, auth_url, verify_url, username, password_encrypted, domain_name,
    active, connect_timeout_ms, read_timeout_ms, token_refresh_skew_sec,
    updated_by, updated_at, created_at
) values (
    1,
    'https://staging-db.yem.internal/ords/dl_user_management/authentication/latest/authenticate',
    'https://staging-db.yem.internal/ords/rest/national-id/v1/verify',
    'NATVERIFIAPI',
    'T9KSa-e8876H!',
    'UM_MASTER',
    false,
    10000,
    30000,
    60,
    'system',
    now(6),
    now(6)
);
