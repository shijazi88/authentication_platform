package com.middleware.platform.iam.dto;

import java.util.List;
import java.util.UUID;

/** A tenant's subscription with its plan and entitlement details. */
public record SubscriptionDetailResponse(
        UUID subscriptionId,
        String status,
        String startDate,
        String endDate,
        String planCode,
        String planName,
        String planDescription,
        long baseFeeMinor,
        String currency,
        List<PlanOperationView> operations,
        List<String> visibleFields
) {}
