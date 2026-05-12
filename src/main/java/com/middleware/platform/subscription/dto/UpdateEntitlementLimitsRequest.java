package com.middleware.platform.subscription.dto;

/**
 * Updates the throttling configuration of a {@code PlanEntitlement}.
 *
 * <p>Both fields are nullable: {@code null} means "unlimited" (no enforcement).
 * A positive value sets the cap. Negative/zero is rejected.
 */
public record UpdateEntitlementLimitsRequest(
        Integer rateLimitPerMinute,
        Long monthlyQuota
) {}
