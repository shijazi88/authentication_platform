-- ============================================================================
-- V5: Billing events
-- ============================================================================

create table billing_events (
    id                  varchar(36) not null,
    transaction_id      varchar(36) not null,
    tenant_id           varchar(36) not null,
    subscription_id     varchar(36) not null,
    service_id          varchar(36) not null,
    operation_id        varchar(36) not null,
    unit_price_minor    bigint not null,
    amount_minor        bigint not null,
    currency            varchar(3) not null,
    period              varchar(7) not null,
    occurred_at         datetime(6) not null,
    primary key (id),
    unique key uk_billing_transaction (transaction_id),
    key idx_billing_tenant_period (tenant_id, period),
    constraint fk_billing_tenant foreign key (tenant_id)
        references tenants(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
