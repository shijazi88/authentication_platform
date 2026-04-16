package com.middleware.platform.connector.spi;

/**
 * SPI implemented by every backend verification provider integration.
 * <p>One connector per provider; operations are dispatched via the {@code operationCode}
 * inside the {@link ConnectorRequest}.
 */
public interface VerificationConnector {

    /**
     * Stable key matching {@code service_definitions.connector_key}.
     * Examples: {@code YEMEN_ID}, {@code PASSPORT_AUTHORITY}.
     */
    String key();

    ConnectorResponse invoke(ConnectorRequest request);
}
