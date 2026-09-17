package com.middleware.platform.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Wrapper returned to banks. {@code result} is the projected canonical payload —
 * its content depends on the tenant's plan field entitlements.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerifyIdentityResponse(
        Transaction transaction,
        Map<String, Object> result,
        /** NFIQ 2 fingerprint quality score (0–100) measured by the platform; absent when not measured or not shared. */
        Integer imageQuality
) {
    public VerifyIdentityResponse(Transaction transaction, Map<String, Object> result) {
        this(transaction, result, null);
    }

    public record Transaction(UUID id, Instant timestamp, String status) {}
}
