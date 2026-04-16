package com.middleware.platform.gateway.orchestrator;

import com.middleware.platform.catalog.domain.ServiceDefinition;
import com.middleware.platform.catalog.domain.ServiceOperation;
import com.middleware.platform.catalog.service.CatalogService;
import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.common.tenant.TenantContext;
import com.middleware.platform.connector.spi.ConnectorRegistry;
import com.middleware.platform.connector.spi.ConnectorRequest;
import com.middleware.platform.connector.spi.ConnectorResponse;
import com.middleware.platform.connector.spi.VerificationConnector;
import com.middleware.platform.gateway.projection.FieldProjector;
import com.middleware.platform.subscription.dto.ResolvedEntitlement;
import com.middleware.platform.subscription.service.EntitlementService;
import com.middleware.platform.transactions.domain.Transaction;
import com.middleware.platform.transactions.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Centralizes the request flow for every bank verification call:
 * authenticate (already done by filter) → resolve entitlement → look up the connector →
 * persist transaction → invoke backend → project the response → emit billing event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationOrchestrator {

    private final CatalogService catalogService;
    private final EntitlementService entitlementService;
    private final ConnectorRegistry connectorRegistry;
    private final TransactionService transactionService;
    private final FieldProjector fieldProjector;
    private final RateLimiter rateLimiter;

    public OrchestrationResult execute(String serviceCode,
                                       String operationCode,
                                       Map<String, Object> canonicalRequestPayload) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        if (tenant == null) {
            throw new ApplicationException(ErrorCode.UNAUTHENTICATED, "No tenant in context");
        }

        ServiceDefinition service = catalogService.getServiceByCode(serviceCode);
        ServiceOperation operation = catalogService.getOperation(serviceCode, operationCode);

        ResolvedEntitlement entitlement;
        try {
            entitlement = entitlementService.resolve(tenant.tenantId(), serviceCode, operationCode);
        } catch (ApplicationException ex) {
            transactionService.rejectEntitlement(tenant.tenantId(), tenant.credentialId(),
                    service.getId(), operation.getId(), ex.getMessage());
            throw ex;
        }

        // Rate limit + monthly quota enforcement (throws QUOTA_EXCEEDED / 429).
        try {
            rateLimiter.check(tenant.credentialId(), tenant.tenantId(),
                    entitlement.operationId(),
                    entitlement.rateLimitPerMinute(),
                    entitlement.monthlyQuota());
        } catch (ApplicationException ex) {
            transactionService.rejectEntitlement(tenant.tenantId(), tenant.credentialId(),
                    service.getId(), operation.getId(), ex.getMessage());
            throw ex;
        }

        if (!service.isActive() || !operation.isActive()) {
            String reason = !service.isActive()
                    ? "Service " + serviceCode + " is not active"
                    : "Operation " + serviceCode + "/" + operationCode + " is not active";
            transactionService.rejectInactive(tenant.tenantId(), tenant.credentialId(),
                    service.getId(), operation.getId(), reason);
            throw new ApplicationException(ErrorCode.CONNECTOR_UNAVAILABLE, reason);
        }

        VerificationConnector connector = connectorRegistry.require(service.getConnectorKey());

        Transaction tx = transactionService.begin(
                tenant.tenantId(),
                tenant.credentialId(),
                entitlement.subscriptionId(),
                service.getId(),
                operation.getId(),
                entitlement.unitPriceMinor(),
                entitlement.currency()
        );

        ConnectorRequest connectorRequest = new ConnectorRequest(
                operationCode,
                tenant.tenantId(),
                tx.getId(),
                canonicalRequestPayload,
                Map.of()
        );

        ConnectorResponse connectorResponse;
        try {
            connectorResponse = connector.invoke(connectorRequest);
        } catch (ApplicationException ex) {
            transactionService.completeFailed(tx, ex.getErrorCode(), ex.getMessage(),
                    canonicalRequestPayload, connectorRequest, null);
            throw ex;
        } catch (Exception ex) {
            log.error("Connector {} failed", service.getConnectorKey(), ex);
            transactionService.completeFailed(tx, ErrorCode.CONNECTOR_ERROR, ex.getMessage(),
                    canonicalRequestPayload, connectorRequest, null);
            throw new ApplicationException(ErrorCode.CONNECTOR_ERROR,
                    "Backend connector error: " + ex.getMessage(), ex);
        }

        Map<String, Object> projected = fieldProjector.project(
                connectorResponse.payload(),
                entitlement.visibleFieldPaths()
        );

        transactionService.completeSuccess(tx,
                connectorResponse.providerRequestId(),
                connectorResponse.latencyMs(),
                canonicalRequestPayload,
                projected,
                connectorRequest,
                connectorResponse.payload());

        return new OrchestrationResult(tx.getId(), Instant.now(), projected);
    }

    public record OrchestrationResult(UUID transactionId, Instant timestamp, Map<String, Object> projected) {}
}
