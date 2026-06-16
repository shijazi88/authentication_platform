package com.middleware.platform.iam.dto;

/** One entitled operation within a plan, as shown to the tenant. */
public record PlanOperationView(
        String operationCode,
        String operationName,
        Integer rateLimitPerMinute,
        Long monthlyQuota,
        long unitPriceMinor,
        String currency
) {}
