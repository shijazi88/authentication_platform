-- ============================================================================
-- V4: Transaction store + payload cold storage
-- ============================================================================

create table transactions (
    id                      varchar(36) not null,
    tenant_id               varchar(36) not null,
    credential_id           varchar(36) not null,
    subscription_id         varchar(36),
    service_id              varchar(36) not null,
    operation_id            varchar(36) not null,
    status                  varchar(32) not null,
    provider_request_id     varchar(64),
    latency_ms              bigint,
    error_code              int,
    error_message           varchar(1024),
    unit_price_minor        bigint,
    currency                varchar(3),
    billable                boolean not null default false,
    created_at              datetime(6) not null,
    primary key (id),
    key idx_tx_tenant_created (tenant_id, created_at),
    key idx_tx_subscription (subscription_id),
    key idx_tx_operation (operation_id),
    constraint fk_tx_tenant foreign key (tenant_id)
        references tenants(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table transaction_payloads (
    transaction_id          varchar(36) not null,
    tenant_request_json     longtext,
    tenant_response_json    longtext,
    provider_request_json   longtext,
    provider_response_json  longtext,
    primary key (transaction_id),
    constraint fk_tp_transaction foreign key (transaction_id)
        references transactions(id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
