package com.middleware.platform.transactions.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_tenant_created", columnList = "tenant_id, created_at"),
        @Index(name = "idx_tx_subscription", columnList = "subscription_id"),
        @Index(name = "idx_tx_operation", columnList = "operation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    /** UUID v7 — assigned by the gateway for traceability across the platform. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "credential_id", nullable = false)
    private UUID credentialId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TransactionStatus status;

    @Column(name = "provider_request_id", length = 64)
    private String providerRequestId;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "error_code")
    private Integer errorCode;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "unit_price_minor")
    private Long unitPriceMinor;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "billable", nullable = false)
    private boolean billable;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
