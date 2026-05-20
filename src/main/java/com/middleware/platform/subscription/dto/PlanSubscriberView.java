package com.middleware.platform.subscription.dto;

import com.middleware.platform.subscription.domain.SubscriptionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One row in the "who joined this plan" list shown on the plan detail
 * page. Joins {@link com.middleware.platform.subscription.domain.Subscription}
 * with {@link com.middleware.platform.iam.domain.Tenant} so the UI can render
 * a client name without a second round-trip.
 */
public record PlanSubscriberView(
        UUID subscriptionId,
        UUID tenantId,
        String tenantCode,
        String tenantLegalName,
        SubscriptionStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt
) {}
