-- ============================================================================
-- V10: MOI API call audit log — every outbound auth/verify call to MOI is
-- recorded (redacted request + full response + latency + error). Used for
-- debugging integration issues and post-mortem. Bodies truncated at 64 KiB
-- by the application layer. Retention is handled by a scheduled cleanup.
-- ============================================================================

create table moi_api_calls (
    id                       varchar(36)  not null,
    created_at               datetime(6)  not null,
    kind                     varchar(16)  not null,             -- AUTH | VERIFY
    transaction_id           varchar(36),                       -- fk to transactions (nullable)
    tenant_id                varchar(36),                       -- fk to tenants (nullable)
    url                      varchar(1024) not null,
    method                   varchar(8)   not null default 'POST',
    request_headers_json     mediumtext,
    request_body_json        mediumtext,
    http_status              int,
    response_headers_json    mediumtext,
    response_body_json       mediumtext,
    duration_ms              bigint,
    error_message            varchar(2048),
    token_snippet            varchar(128),                      -- non-sensitive JWT prefix/suffix for correlation
    primary key (id),
    key idx_moi_calls_kind_created (kind, created_at),
    key idx_moi_calls_created (created_at),
    key idx_moi_calls_txn (transaction_id),
    key idx_moi_calls_tenant_created (tenant_id, created_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
