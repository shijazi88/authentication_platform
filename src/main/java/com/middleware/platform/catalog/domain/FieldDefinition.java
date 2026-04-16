package com.middleware.platform.catalog.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * A response field that the gateway can return for a given operation. Drives field-level
 * masking — a subscription must include the corresponding plan_field_entitlement to receive
 * this field.
 *
 * <p>Path uses dot notation against the canonical response, e.g.
 *   verification.status
 *   person.demographics.names.arabic.first
 *   person.cards[*].documentNumber
 */
@Entity
@Table(name = "field_definitions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"operation_id", "path"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldDefinition {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "path", nullable = false, length = 255)
    private String path;

    @Column(name = "data_class", length = 64)
    private String dataClass;

    @Column(name = "description", length = 512)
    private String description;
}
