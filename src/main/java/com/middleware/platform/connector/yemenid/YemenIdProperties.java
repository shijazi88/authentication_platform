package com.middleware.platform.connector.yemenid;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.connectors.yemen-id")
public record YemenIdProperties(
        String baseUrl,
        String bearerToken,
        long timeoutMs
) {}
