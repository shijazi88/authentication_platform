package com.middleware.platform.iam.dto;

import com.middleware.platform.common.error.BankFacingMessages;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.transactions.domain.Transaction;
import com.middleware.platform.transactions.domain.TransactionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * A transaction as shown to the bank in its own portal: same fields as the
 * entity, but the error text is rewritten in plain language (no provider
 * names, HTTP statuses or infrastructure terms).
 */
public record TenantTransactionView(
        UUID id, UUID tenantId, UUID credentialId, UUID subscriptionId, UUID serviceId, UUID operationId,
        TransactionStatus status, String providerRequestId, Long latencyMs,
        Integer errorCode, String errorMessage,
        Long unitPriceMinor, String currency, boolean billable,
        boolean exception, String exceptionReason, String exceptionNote,
        String imageFormat, Integer imageWidth, Integer imageHeight, Integer imagePpi, Integer imageBytes,
        String imageCheck, String imageCheckMessage,
        Instant createdAt
) {
    public static TenantTransactionView from(Transaction t) {
        return new TenantTransactionView(
                t.getId(), t.getTenantId(), t.getCredentialId(), t.getSubscriptionId(), t.getServiceId(), t.getOperationId(),
                t.getStatus(), t.getProviderRequestId(), t.getLatencyMs(),
                t.getErrorCode(), publicMessage(t.getErrorCode(), t.getErrorMessage()),
                t.getUnitPriceMinor(), t.getCurrency(), t.isBillable(),
                t.isException(), t.getExceptionReason(), t.getExceptionNote(),
                t.getImageFormat(), t.getImageWidth(), t.getImageHeight(), t.getImagePpi(), t.getImageBytes(),
                t.getImageCheck(), t.getImageCheckMessage(),
                t.getCreatedAt());
    }

    private static String publicMessage(Integer code, String internal) {
        if (internal == null) return null;
        for (ErrorCode ec : ErrorCode.values()) {
            if (code != null && ec.code() == code) return BankFacingMessages.forBank(ec, internal);
        }
        return internal.contains("MOI") ? internal.replace("MOI", "the verification service") : internal;
    }
}
