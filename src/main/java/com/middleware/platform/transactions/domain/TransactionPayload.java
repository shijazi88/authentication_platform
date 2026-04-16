package com.middleware.platform.transactions.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Cold storage for the request and response payloads associated with a transaction.
 * Stored separately so the hot transactions table stays small and so PII can be
 * purged on a different retention schedule than the audit row.
 *
 * <p>For v1 these are stored as JSONB strings. In production, encrypt with KMS-issued
 * data keys before persistence.
 */
@Entity
@Table(name = "transaction_payloads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionPayload {

    @Id
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "tenant_request_json", columnDefinition = "longtext")
    private String tenantRequestJson;

    @Column(name = "tenant_response_json", columnDefinition = "longtext")
    private String tenantResponseJson;

    @Column(name = "provider_request_json", columnDefinition = "longtext")
    private String providerRequestJson;

    @Column(name = "provider_response_json", columnDefinition = "longtext")
    private String providerResponseJson;
}
