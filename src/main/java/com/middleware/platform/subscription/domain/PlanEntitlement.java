package com.middleware.platform.subscription.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Per-operation entitlement on a plan: which operation is allowed, with what quota,
 * pricing override, and rate limit.
 */
@Entity
@Table(name = "plan_entitlements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "operation_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanEntitlement {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    /** Optional override of operation default unit price (minor units). null = use default. */
    @Column(name = "unit_price_override")
    private Long unitPriceOverrideMinor;

    /** Monthly transaction quota; null = unlimited. */
    @Column(name = "monthly_quota")
    private Long monthlyQuota;

    /** Per-minute rate limit; null = unlimited. */
    @Column(name = "rate_limit_per_minute")
    private Integer rateLimitPerMinute;
}
