package com.middleware.platform.subscription.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Drives the "hit-only vs hit-with-data" feature: lists which response fields a plan exposes.
 * Any path not whitelisted here is stripped from the response before returning to the tenant.
 */
@Entity
@Table(name = "plan_field_entitlements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "field_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanFieldEntitlement {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "field_id", nullable = false)
    private UUID fieldId;

    /** Cached path string for fast lookup without joining. */
    @Column(name = "field_path", nullable = false, length = 255)
    private String fieldPath;
}
