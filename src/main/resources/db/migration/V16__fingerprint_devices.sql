-- =============================================================================
-- V16 — Fingerprint devices, registered per tenant (client).
--
-- Each device belongs to one tenant. Serial numbers are unique across active
-- (non-deleted) devices; deletion is soft (deleted flag + deleted_at) so a
-- device's history is retained and its serial can be re-registered later.
-- =============================================================================

create table fingerprint_devices (
    id             varchar(36)  not null,
    tenant_id      varchar(36)  not null,
    name           varchar(255) not null,
    model          varchar(255),
    type           varchar(64),
    serial_number  varchar(128) not null,
    deleted        boolean      not null default false,
    deleted_at     datetime(6),
    created_by     varchar(255),
    created_at     datetime(6)  not null,
    updated_at     datetime(6)  not null,
    primary key (id),
    key idx_fp_devices_tenant (tenant_id, deleted),
    key idx_fp_devices_serial (serial_number),
    constraint fk_fp_devices_tenant foreign key (tenant_id) references tenants (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
