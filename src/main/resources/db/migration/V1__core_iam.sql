-- ============================================================================
-- V1: Core IAM tables — tenants, API credentials, admin users
-- MySQL 8 dialect.
-- ============================================================================

create table tenants (
    id              varchar(36) not null,
    code            varchar(64) not null,
    legal_name      varchar(255) not null,
    contact_email   varchar(255),
    status          varchar(32) not null,
    created_at      datetime(6) not null,
    updated_at      datetime(6) not null,
    primary key (id),
    unique key uk_tenants_code (code)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table api_credentials (
    id                  varchar(36) not null,
    tenant_id           varchar(36) not null,
    client_id           varchar(64) not null,
    client_secret_hash  varchar(255) not null,
    label               varchar(128),
    ip_allowlist        varchar(1024),
    active              boolean not null default true,
    expires_at          datetime(6),
    created_at          datetime(6) not null,
    last_used_at        datetime(6),
    primary key (id),
    unique key uk_credentials_client_id (client_id),
    key idx_api_credentials_tenant (tenant_id),
    constraint fk_credentials_tenant foreign key (tenant_id)
        references tenants(id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table admin_users (
    id              varchar(36) not null,
    email           varchar(255) not null,
    password_hash   varchar(255) not null,
    display_name    varchar(255),
    role            varchar(32) not null,
    active          boolean not null default true,
    created_at      datetime(6) not null,
    last_login_at   datetime(6),
    primary key (id),
    unique key uk_admin_users_email (email)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
