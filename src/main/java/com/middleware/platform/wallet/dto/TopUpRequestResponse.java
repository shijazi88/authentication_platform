package com.middleware.platform.wallet.dto;

import com.middleware.platform.wallet.domain.TopUpRequestStatus;
import com.middleware.platform.wallet.domain.WalletTopUpRequest;

import java.time.Instant;
import java.util.UUID;

public record TopUpRequestResponse(
        UUID id,
        UUID tenantId,
        long amountMinor,
        String currency,
        String note,
        TopUpRequestStatus status,
        String requestedBy,
        String decidedBy,
        String decidedNote,
        Instant createdAt,
        Instant decidedAt
) {
    public static TopUpRequestResponse from(WalletTopUpRequest r) {
        return new TopUpRequestResponse(
                r.getId(), r.getTenantId(), r.getAmountMinor(), r.getCurrency(), r.getNote(),
                r.getStatus(), r.getRequestedBy(), r.getDecidedBy(), r.getDecidedNote(),
                r.getCreatedAt(), r.getDecidedAt());
    }
}
