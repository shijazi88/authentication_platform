package com.middleware.platform.connector.spi;

import java.util.Map;

/**
 * Connector response in canonical form, ready for field-level projection by the gateway.
 */
public record ConnectorResponse(
        int providerHttpStatus,
        String providerRequestId,
        Map<String, Object> payload,
        long latencyMs
) {}
