package com.middleware.platform.connector.spi;

import java.util.Map;
import java.util.UUID;

/**
 * Generic request passed from the gateway to a connector. The payload is a canonical
 * (provider-agnostic) shape; the connector translates it to the provider's wire format.
 */
public record ConnectorRequest(
        String operationCode,
        UUID tenantId,
        UUID requestId,
        Map<String, Object> payload,
        Map<String, String> metadata
) {}
