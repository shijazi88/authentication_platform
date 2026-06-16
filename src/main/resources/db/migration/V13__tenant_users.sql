-- =============================================================================
-- V13 — Tenant portal users.
--
-- Human logins for the tenant-facing portal (banks). Distinct from
-- api_credentials (machine-to-machine) and admin_users (platform staff). Each
-- user belongs to exactly one tenant and only ever sees that tenant's data.
-- =============================================================================

create table tenant_users (
    id              varchar(36)  not null,
    tenant_id       varchar(36)  not null,
    email           varchar(255) not null,
    password_hash   varchar(255) not null,
    display_name    varchar(255),
    active          boolean      not null default true,
    created_at      datetime(6)  not null,
    last_login_at   datetime(6),
    primary key (id),
    unique key uk_tenant_users_email (email),
    key idx_tenant_users_tenant (tenant_id),
    constraint fk_tenant_users_tenant foreign key (tenant_id) references tenants (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
