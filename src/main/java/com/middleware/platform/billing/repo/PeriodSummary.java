package com.middleware.platform.billing.repo;

import java.util.UUID;

public record PeriodSummary(
        UUID tenantId,
        String period,
        String currency,
        Long totalAmountMinor,
        Long transactionCount
) {}
