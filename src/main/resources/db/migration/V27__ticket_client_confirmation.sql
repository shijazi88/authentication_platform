-- =============================================================================
-- V27 — Client confirmation of solved tickets
--
-- When the support team marks a ticket RESOLVED it now waits for the client:
-- the client confirms (→ CLOSED, closed_by = CLIENT) or replies, which reopens
-- it. A RESOLVED ticket the client never answers is closed automatically after
-- platform.tickets.auto-close-days (closed_by = AUTO). Staff can still close a
-- ticket directly (closed_by = STAFF).
-- =============================================================================
alter table support_tickets
    add column closed_by varchar(16) null after closed_at,   -- CLIENT | STAFF | AUTO
    add key idx_ticket_resolved (status, resolved_at);
