-- ============================================================================
-- V8: Audit log — immutable record of every admin action.
-- ============================================================================

create table audit_log (
    id              varchar(36) not null,
    actor_id        varchar(36),
    actor_email     varchar(255),
    action          varchar(64) not null,
    target_type     varchar(64) not null,
    target_id       varchar(36),
    detail          varchar(2048),
    ip_address      varchar(64),
    created_at      datetime(6) not null,
    primary key (id),
    key idx_audit_actor (actor_id),
    key idx_audit_target (target_type, target_id),
    key idx_audit_created (created_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
