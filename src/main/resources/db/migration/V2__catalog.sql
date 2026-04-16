-- ============================================================================
-- V2: Service catalog — backend services, operations, response field dictionary
-- ============================================================================

create table service_definitions (
    id              varchar(36) not null,
    code            varchar(64) not null,
    name            varchar(255) not null,
    description     varchar(1024),
    connector_key   varchar(64) not null,
    active          boolean not null default true,
    created_at      datetime(6) not null,
    primary key (id),
    unique key uk_service_definitions_code (code)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table service_operations (
    id                       varchar(36) not null,
    service_id               varchar(36) not null,
    code                     varchar(64) not null,
    name                     varchar(255) not null,
    description              varchar(1024),
    default_unit_price       bigint not null,
    currency                 varchar(3) not null,
    active                   boolean not null default true,
    created_at               datetime(6) not null,
    primary key (id),
    unique key uk_op_service_code (service_id, code),
    key idx_op_service (service_id),
    constraint fk_operation_service foreign key (service_id)
        references service_definitions(id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table field_definitions (
    id              varchar(36) not null,
    operation_id    varchar(36) not null,
    path            varchar(255) not null,
    data_class      varchar(64),
    description     varchar(512),
    primary key (id),
    unique key uk_field_op_path (operation_id, path),
    key idx_field_operation (operation_id),
    constraint fk_field_operation foreign key (operation_id)
        references service_operations(id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
