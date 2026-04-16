package com.middleware.platform.subscription.dto;

import java.util.Set;
import java.util.UUID;

/**
 * Snapshot of what a tenant is entitled to for a particular operation, used by the gateway
 * during request handling.
 */
public record ResolvedEntitlement(
        UUID subscriptionId,
        UUID planId,
        UUID operationId,
        long unitPriceMinor,
        String currency,
        Long monthlyQuota,
        Integer rateLimitPerMinute,
        Set<String> visibleFieldPaths
) {}
