package com.middleware.platform.transactions.event;

import com.middleware.platform.transactions.domain.TransactionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published whenever a transaction reaches a terminal state.
 * Listened to by the billing module (and any future module that wants
 * derived data without coupling to the gateway).
 *
 * <p>v1: in-process Spring application event. v2: replace with Kafka outbox
 * pattern, no producer changes required.
 */
public record TransactionCompletedEvent(
        UUID transactionId,
        UUID tenantId,
        UUID subscriptionId,
        UUID serviceId,
        UUID operationId,
        TransactionStatus status,
        boolean billable,
        long unitPriceMinor,
        String currency,
        Instant occurredAt
) {}
