-- =============================================================================
-- V26 — Support tickets
--
-- A client (tenant portal user) opens a ticket; the support team answers it
-- from the admin portal. Each ticket is a thread of messages. Staff can add
-- internal notes (internal = true) that the client never sees.
--
-- ticket_no is the short, human number shown to both sides ("#1001"). It is a
-- separate auto-increment column because the primary key stays a UUID like
-- every other table.
-- =============================================================================

create table support_tickets (
    id               varchar(36)  not null,
    ticket_no        bigint       not null auto_increment,
    tenant_id        varchar(36)  not null,
    subject          varchar(200) not null,
    category         varchar(32)  not null,                    -- VERIFICATION | BILLING | INTEGRATION | ACCOUNT | OTHER
    priority         varchar(16)  not null default 'NORMAL',   -- LOW | NORMAL | HIGH | URGENT
    status           varchar(24)  not null default 'OPEN',     -- OPEN | IN_PROGRESS | WAITING_ON_CLIENT | RESOLVED | CLOSED
    reference        varchar(128),                             -- optional request / transaction id the client quotes
    created_by       varchar(255) not null,                    -- client portal user email
    assigned_to      varchar(255),                             -- admin email
    last_message_at  datetime(6)  not null,
    last_message_by  varchar(16)  not null,                    -- CLIENT | STAFF (who spoke last, public messages only)
    created_at       datetime(6)  not null,
    updated_at       datetime(6)  not null,
    resolved_at      datetime(6),
    closed_at        datetime(6),
    primary key (id),
    unique key uk_ticket_no (ticket_no),
    key idx_ticket_tenant (tenant_id, last_message_at),
    key idx_ticket_status (status, last_message_at),
    constraint fk_ticket_tenant foreign key (tenant_id) references tenants (id) on delete cascade
) engine=InnoDB auto_increment=1001 default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table support_ticket_messages (
    id            varchar(36)  not null,
    ticket_id     varchar(36)  not null,
    author_type   varchar(16)  not null,                       -- CLIENT | STAFF
    author_email  varchar(255) not null,
    body          text         not null,
    internal      boolean      not null default false,         -- staff-only note, hidden from the client
    created_at    datetime(6)  not null,
    primary key (id),
    key idx_ticket_msg_ticket (ticket_id, created_at),
    constraint fk_ticket_msg_ticket foreign key (ticket_id) references support_tickets (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
