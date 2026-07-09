-- =============================================================================
-- V17 — Client-initiated wallet top-up requests.
--
-- A tenant (client) requests a wallet charge; a platform admin approves it
-- (which credits the wallet) or rejects it. Clients cannot credit their own
-- wallet directly, so this is the request/approval trail.
-- =============================================================================

create table wallet_topup_requests (
    id            varchar(36)  not null,
    tenant_id     varchar(36)  not null,
    amount_minor  bigint       not null,
    currency      varchar(3)   not null default 'YER',
    note          varchar(512),
    status        varchar(16)  not null default 'PENDING',  -- PENDING | APPROVED | REJECTED
    requested_by  varchar(255),
    decided_by    varchar(255),
    decided_note  varchar(512),
    created_at    datetime(6)  not null,
    decided_at    datetime(6),
    primary key (id),
    key idx_topup_req_tenant (tenant_id, created_at),
    key idx_topup_req_status (status, created_at),
    constraint fk_topup_req_tenant foreign key (tenant_id) references tenants (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
