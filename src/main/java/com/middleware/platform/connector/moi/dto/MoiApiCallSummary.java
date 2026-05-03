package com.middleware.platform.connector.moi.dto;

import com.middleware.platform.connector.moi.domain.MoiApiCall;

import java.time.Instant;

/** Compact view for the audit call list — no bodies. */
public record MoiApiCallSummary(
        String id,
        Instant createdAt,
        String kind,
        String url,
        Integer httpStatus,
        Long durationMs,
        String tokenSnippet,
        String errorMessage,
        String tenantId,
        String transactionId
) {
    public static MoiApiCallSummary from(MoiApiCall c) {
        return new MoiApiCallSummary(
                c.getId(),
                c.getCreatedAt(),
                c.getKind() != null ? c.getKind().name() : null,
                c.getUrl(),
                c.getHttpStatus(),
                c.getDurationMs(),
                c.getTokenSnippet(),
                c.getErrorMessage(),
                c.getTenantId(),
                c.getTransactionId()
        );
    }
}
