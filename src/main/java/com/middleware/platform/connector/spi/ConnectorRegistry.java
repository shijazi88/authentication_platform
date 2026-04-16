package com.middleware.platform.connector.spi;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Discovers all VerificationConnector beans and looks them up by key.
 * Adding a new backend = drop in a new {@code @Component} implementing
 * {@link VerificationConnector}.
 */
@Component
public class ConnectorRegistry {

    private final Map<String, VerificationConnector> connectors;

    public ConnectorRegistry(List<VerificationConnector> beans) {
        this.connectors = beans.stream().collect(Collectors.toMap(
                VerificationConnector::key,
                Function.identity()
        ));
    }

    public VerificationConnector require(String key) {
        VerificationConnector c = connectors.get(key);
        if (c == null) {
            throw new ApplicationException(ErrorCode.CONNECTOR_UNAVAILABLE,
                    "No connector registered for key: " + key);
        }
        return c;
    }
}
