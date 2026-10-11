-- =============================================================================
-- V25 — Unlimited wallet per tenant + audit action length
-- =============================================================================
-- wallet_unlimited: FALSE → prepaid as before: a verification the balance can't
--                   cover is rejected (402 / 1403 INSUFFICIENT_FUNDS).
--                   TRUE  → the service never stops for lack of balance. Each
--                   charge is still debited, so the balance can go negative —
--                   the negative amount is what the bank owes.
alter table tenants
    add column wallet_unlimited boolean not null default false;

-- audit_log.action was varchar(64): "PUT /admin/tenants/<uuid>/ip-policy" (65
-- chars) and longer tenant actions failed to insert, and the audit aspect
-- swallowed the error — those admin changes were never audited.
alter table audit_log
    modify column action varchar(255) not null;
