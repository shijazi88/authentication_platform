package com.middleware.platform.transactions.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.common.util.Ids;
import com.middleware.platform.transactions.domain.Transaction;
import com.middleware.platform.transactions.domain.TransactionPayload;
import com.middleware.platform.transactions.domain.TransactionStatus;
import com.middleware.platform.transactions.event.TransactionCompletedEvent;
import com.middleware.platform.transactions.repo.TransactionPayloadRepository;
import com.middleware.platform.transactions.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionPayloadRepository payloadRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public Transaction begin(UUID tenantId, UUID credentialId, UUID subscriptionId,
                             UUID serviceId, UUID operationId,
                             long unitPriceMinor, String currency) {
        Transaction tx = Transaction.builder()
                .id(Ids.uuidV7())
                .tenantId(tenantId)
                .credentialId(credentialId)
                .subscriptionId(subscriptionId)
                .serviceId(serviceId)
                .operationId(operationId)
                .status(TransactionStatus.INITIATED)
                .unitPriceMinor(unitPriceMinor)
                .currency(currency)
                .billable(false)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(tx);
        return tx;
    }

    @Transactional
    public void completeSuccess(Transaction tx, String providerRequestId, long latencyMs,
                                Object tenantRequest, Object tenantResponse,
                                Object providerRequest, Object providerResponse) {
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setProviderRequestId(providerRequestId);
        tx.setLatencyMs(latencyMs);
        tx.setBillable(true);
        transactionRepository.save(tx);
        savePayloads(tx.getId(), tenantRequest, tenantResponse, providerRequest, providerResponse);
        publish(tx);
    }

    @Transactional
    public void completeFailed(Transaction tx, ErrorCode errorCode, String message,
                               Object tenantRequest, Object tenantResponse,
                               Object providerRequest, Object providerResponse) {
        tx.setStatus(errorCode == ErrorCode.CONNECTOR_TIMEOUT
                ? TransactionStatus.TIMEOUT
                : TransactionStatus.FAILED);
        tx.setErrorCode(errorCode.code());
        tx.setErrorMessage(safeTrim(message, 1024));
        tx.setBillable(false);
        transactionRepository.save(tx);
        savePayloads(tx.getId(), tenantRequest, tenantResponse, providerRequest, providerResponse);
        publish(tx);
    }

    @Transactional
    public void rejectEntitlement(UUID tenantId, UUID credentialId, UUID serviceId, UUID operationId,
                                  String message) {
        recordRejection(tenantId, credentialId, serviceId, operationId,
                ErrorCode.ENTITLEMENT_DENIED, message);
    }

    /**
     * Records a REJECTED transaction when the requested service or operation is
     * disabled in the catalog. Same audit shape as an entitlement rejection but
     * with a different error code so reports can distinguish them.
     */
    @Transactional
    public void rejectInactive(UUID tenantId, UUID credentialId, UUID serviceId, UUID operationId,
                               String message) {
        recordRejection(tenantId, credentialId, serviceId, operationId,
                ErrorCode.CONNECTOR_UNAVAILABLE, message);
    }

    /**
     * Records a REJECTED transaction when the tenant's prepaid wallet can't
     * cover the call. Distinct error code so reports can separate funding
     * rejections from entitlement/quota ones.
     */
    @Transactional
    public void rejectInsufficientFunds(UUID tenantId, UUID credentialId, UUID serviceId, UUID operationId,
                                        String message) {
        recordRejection(tenantId, credentialId, serviceId, operationId,
                ErrorCode.INSUFFICIENT_FUNDS, message);
    }

    private void recordRejection(UUID tenantId, UUID credentialId, UUID serviceId, UUID operationId,
                                 ErrorCode errorCode, String message) {
        Transaction tx = Transaction.builder()
                .id(Ids.uuidV7())
                .tenantId(tenantId)
                .credentialId(credentialId)
                .serviceId(serviceId)
                .operationId(operationId)
                .status(TransactionStatus.REJECTED)
                .errorCode(errorCode.code())
                .errorMessage(safeTrim(message, 1024))
                .billable(false)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(tx);
        // No event — rejected transactions are not billable.
    }

    @Transactional(readOnly = true)
    public Page<Transaction> listByTenant(UUID tenantId, Pageable pageable) {
        return transactionRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> listAll(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    /** Admin listing with optional client / status / response-code / billable / exception / id / date filters. */
    @Transactional(readOnly = true)
    public Page<Transaction> filter(UUID tenantId, TransactionStatus status, Integer errorCode,
                                    Boolean billable, Boolean exception, String idQuery,
                                    Instant from, Instant to, Pageable pageable) {
        String idq = (idQuery == null || idQuery.isBlank()) ? null : idQuery.trim();
        return transactionRepository.filter(tenantId, status, errorCode, billable, exception, idq, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Transaction get(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> ApplicationException.notFound("Transaction"));
    }

    private void savePayloads(UUID txId, Object tenantReq, Object tenantRes,
                              Object providerReq, Object providerRes) {
        TransactionPayload payload = TransactionPayload.builder()
                .transactionId(txId)
                .tenantRequestJson(toJson(tenantReq))
                .tenantResponseJson(toJson(tenantRes))
                .providerRequestJson(toJson(providerReq))
                .providerResponseJson(toJson(providerRes))
                .build();
        payloadRepository.save(payload);
    }

    /** Field names whose values must never be persisted in clear. */
    private static final java.util.Set<String> SENSITIVE_KEYS = java.util.Set.of(
            "image", "nationalnumber", "fingerprint", "fingerprintimage", "photo", "biometricimage");
    private static final String REDACTED = "***REDACTED***";

    /**
     * Serializes to JSON with sensitive PII fields (biometric image, national
     * number, …) masked, so raw biometrics never land in transaction_payloads.
     */
    private String toJson(Object o) {
        if (o == null) return null;
        try {
            JsonNode node = objectMapper.valueToTree(o);
            redact(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Failed to serialize payload: {}", e.getMessage());
            return null;
        }
    }

    private void redact(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fieldNames().forEachRemaining(name -> {
                JsonNode child = obj.get(name);
                if (child != null && child.isValueNode()
                        && SENSITIVE_KEYS.contains(name.toLowerCase().replace("_", ""))) {
                    obj.put(name, REDACTED);
                } else {
                    redact(child);
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::redact);
        }
    }

    private String safeTrim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private void publish(Transaction tx) {
        eventPublisher.publishEvent(new TransactionCompletedEvent(
                tx.getId(),
                tx.getTenantId(),
                tx.getSubscriptionId(),
                tx.getServiceId(),
                tx.getOperationId(),
                tx.getStatus(),
                tx.isBillable(),
                tx.getUnitPriceMinor() == null ? 0L : tx.getUnitPriceMinor(),
                tx.getCurrency(),
                Instant.now()
        ));
    }
}
