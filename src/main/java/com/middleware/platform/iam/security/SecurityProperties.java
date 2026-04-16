package com.middleware.platform.iam.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.security")
public record SecurityProperties(
        Jwt jwt,
        BootstrapAdmin bootstrapAdmin
) {
    public record Jwt(String secret, String issuer, long accessTokenTtlMinutes) {}

    public record BootstrapAdmin(String email, String password, boolean enabled) {}
}
