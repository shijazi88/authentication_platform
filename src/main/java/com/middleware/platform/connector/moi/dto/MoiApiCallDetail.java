package com.middleware.platform.connector.moi.dto;

import com.middleware.platform.connector.moi.domain.MoiApiCall;

import java.time.Instant;

/** Full view including redacted request/response bodies. */
public record MoiApiCallDetail(
        String id,
        Instant createdAt,
        String kind,
        String url,
        String method,
        String requestHeadersJson,
        String requestBodyJson,
        Integer httpStatus,
        String responseHeadersJson,
        String responseBodyJson,
        Long durationMs,
        String tokenSnippet,
        String errorMessage,
        String tenantId,
        String transactionId
) {
    public static MoiApiCallDetail from(MoiApiCall c) {
        return new MoiApiCallDetail(
                c.getId(),
                c.getCreatedAt(),
                c.getKind() != null ? c.getKind().name() : null,
                c.getUrl(),
                c.getMethod(),
                c.getRequestHeadersJson(),
                c.getRequestBodyJson(),
                c.getHttpStatus(),
                c.getResponseHeadersJson(),
                c.getResponseBodyJson(),
                c.getDurationMs(),
                c.getTokenSnippet(),
                c.getErrorMessage(),
                c.getTenantId(),
                c.getTransactionId()
        );
    }
}
