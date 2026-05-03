package com.middleware.platform.transactions.dto;

import com.middleware.platform.transactions.domain.Transaction;
import com.middleware.platform.transactions.domain.TransactionPayload;

import java.time.Instant;
import java.util.UUID;

/**
 * Full view of a transaction for the admin portal — the row from
 * {@code transactions} merged with the JSON bodies from
 * {@code transaction_payloads}. Banks send the {@code tenantRequestJson}
 * and receive the {@code tenantResponseJson}; the {@code providerRequest/
 * Response} are what we sent to / received from MOI before entitlement
 * projection.
 */
public record TransactionDetail(
        UUID id,
        UUID tenantId,
        UUID credentialId,
        UUID subscriptionId,
        UUID serviceId,
        UUID operationId,
        String status,
        String providerRequestId,
        Long latencyMs,
        Integer errorCode,
        String errorMessage,
        Long unitPriceMinor,
        String currency,
        boolean billable,
        Instant createdAt,
        String tenantRequestJson,
        String tenantResponseJson,
        String providerRequestJson,
        String providerResponseJson
) {
    public static TransactionDetail from(Transaction tx, TransactionPayload payload) {
        return new TransactionDetail(
                tx.getId(),
                tx.getTenantId(),
                tx.getCredentialId(),
                tx.getSubscriptionId(),
                tx.getServiceId(),
                tx.getOperationId(),
                tx.getStatus() != null ? tx.getStatus().name() : null,
                tx.getProviderRequestId(),
                tx.getLatencyMs(),
                tx.getErrorCode(),
                tx.getErrorMessage(),
                tx.getUnitPriceMinor(),
                tx.getCurrency(),
                tx.isBillable(),
                tx.getCreatedAt(),
                payload != null ? payload.getTenantRequestJson() : null,
                payload != null ? payload.getTenantResponseJson() : null,
                payload != null ? payload.getProviderRequestJson() : null,
                payload != null ? payload.getProviderResponseJson() : null
        );
    }
}
