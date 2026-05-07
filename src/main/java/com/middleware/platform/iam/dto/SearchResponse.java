package com.middleware.platform.iam.dto;

import com.middleware.platform.transactions.domain.TransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SearchResponse(
        List<TenantResponse> tenants,
        List<TransactionHit> transactions
) {
    public record TransactionHit(
            UUID id,
            TransactionStatus status,
            String providerRequestId,
            Instant createdAt,
            UUID tenantId
    ) {}
}
