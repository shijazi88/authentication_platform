package com.middleware.platform.subscription.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AddPlanEntitlementRequest(
        @NotBlank String serviceCode,
        @NotBlank String operationCode,
        Long unitPriceOverrideMinor,
        Long monthlyQuota,
        Integer rateLimitPerMinute,
        /** Whitelist of response field paths visible under this plan. Empty list = hit-only. */
        List<String> visibleFieldPaths
) {}
