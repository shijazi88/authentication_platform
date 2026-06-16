-- =============================================================================
-- V11 — Prepaid tenant wallet.
--
-- Each tenant has at most one wallet (balance in minor units). Every balance
-- change is recorded as an immutable ledger entry so the balance is always
-- reconstructable and auditable. Billable verification calls debit the wallet
-- atomically; top-ups (admin or payment) credit it.
-- =============================================================================

create table tenant_wallets (
    id              varchar(36)  not null,
    tenant_id       varchar(36)  not null,
    balance_minor   bigint       not null default 0,
    currency        varchar(3)   not null default 'YER',
    -- Optional alert threshold; null = no low-balance alerting.
    low_balance_threshold_minor bigint,
    created_at      datetime(6)  not null,
    updated_at      datetime(6)  not null,
    primary key (id),
    unique key uk_wallet_tenant (tenant_id),
    constraint fk_wallet_tenant foreign key (tenant_id) references tenants (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table wallet_ledger (
    id                   varchar(36)  not null,
    wallet_id            varchar(36)  not null,
    tenant_id            varchar(36)  not null,
    entry_type           varchar(16)  not null,            -- TOPUP | DEBIT | REVERSAL | ADJUSTMENT
    -- Signed amount applied to the balance (credits positive, debits negative).
    amount_minor         bigint       not null,
    balance_after_minor  bigint       not null,
    currency             varchar(3)   not null,
    source               varchar(16)  not null,            -- ADMIN | PAYMENT | SYSTEM
    -- Correlates the entry to its origin: a transaction id (debit), a payment
    -- reference (top-up) or a reversed ledger id.
    reference            varchar(64),
    note                 varchar(512),
    created_by           varchar(255),                     -- admin email, or 'system'
    created_at           datetime(6)  not null,
    primary key (id),
    key idx_ledger_tenant_created (tenant_id, created_at),
    key idx_ledger_wallet_created (wallet_id, created_at),
    key idx_ledger_reference (reference),
    constraint fk_ledger_wallet foreign key (wallet_id) references tenant_wallets (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- Provision a wallet for every existing tenant so prepaid checks have a row to
-- lock. Balance starts at zero (must be topped up before billable calls succeed).
insert into tenant_wallets (id, tenant_id, balance_minor, currency, created_at, updated_at)
select uuid(), t.id, 0, 'YER', now(6), now(6)
from tenants t
where not exists (select 1 from tenant_wallets w where w.tenant_id = t.id);
