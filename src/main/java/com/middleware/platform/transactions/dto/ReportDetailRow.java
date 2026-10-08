package com.middleware.platform.transactions.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * One transaction in the detailed (per-transaction) report. Carries no
 * personal data: the national number and fingerprint never leave the payload store.
 *
 * @param type             FINGERPRINT or EXCEPTION (fingerprint exception, no capture)
 * @param error            error name for {@code errorCode}, e.g. IMAGE_QUALITY_REJECTED
 * @param errorMessage     bank-facing description of the failure
 * @param exceptionNote    free text the bank sent with a fingerprint exception
 * @param amountMinor      amount charged (minor units) — null when not billable
 * @param apiKey           client ID of the API key that made the call
 * @param providerRef      reference returned by the verification provider, for reconciliation
 */
public record ReportDetailRow(
        Instant createdAt,
        UUID transactionId,
        String type,
        String status,
        String verdict,
        Integer errorCode,
        String error,
        String errorMessage,
        String exceptionReason,
        String exceptionNote,
        String deviceId,
        Boolean deviceRegistered,
        String imageFormat,
        Integer imageNfiq2,
        String imageCheck,
        Long latencyMs,
        boolean billable,
        Long amountMinor,
        String currency,
        String apiKey,
        String providerRef
) {}
