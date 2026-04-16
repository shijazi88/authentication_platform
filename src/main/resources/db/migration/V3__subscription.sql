-- ============================================================================
-- V3: Plans, plan entitlements (operation + field), tenant subscriptions
-- ============================================================================

create table plans (
    id              varchar(36) not null,
    code            varchar(64) not null,
    name            varchar(255) not null,
    description     varchar(1024),
    base_fee_minor  bigint not null default 0,
    currency        varchar(3) not null,
    active          boolean not null default true,
    created_at      datetime(6) not null,
    primary key (id),
    unique key uk_plans_code (code)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table plan_entitlements (
    id                      varchar(36) not null,
    plan_id                 varchar(36) not null,
    operation_id            varchar(36) not null,
    unit_price_override     bigint,
    monthly_quota           bigint,
    rate_limit_per_minute   int,
    primary key (id),
    unique key uk_plan_entitlement (plan_id, operation_id),
    key idx_plan_entitlement_plan (plan_id),
    constraint fk_pe_plan foreign key (plan_id)
        references plans(id) on delete cascade,
    constraint fk_pe_operation foreign key (operation_id)
        references service_operations(id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table plan_field_entitlements (
    id          varchar(36) not null,
    plan_id     varchar(36) not null,
    field_id    varchar(36) not null,
    field_path  varchar(255) not null,
    primary key (id),
    unique key uk_plan_field_entitlement (plan_id, field_id),
    key idx_plan_field_entitlement_plan (plan_id),
    constraint fk_pfe_plan foreign key (plan_id)
        references plans(id) on delete cascade,
    constraint fk_pfe_field foreign key (field_id)
        references field_definitions(id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table subscriptions (
    id              varchar(36) not null,
    tenant_id       varchar(36) not null,
    plan_id         varchar(36) not null,
    start_date      date not null,
    end_date        date,
    status          varchar(32) not null,
    created_at      datetime(6) not null,
    primary key (id),
    key idx_subscription_tenant (tenant_id),
    key idx_subscription_status (status),
    constraint fk_sub_tenant foreign key (tenant_id)
        references tenants(id) on delete cascade,
    constraint fk_sub_plan foreign key (plan_id)
        references plans(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
