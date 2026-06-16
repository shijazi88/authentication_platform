package com.middleware.platform.iam.dto;

import java.util.UUID;

public record TenantLoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID tenantId,
        String tenantName,
        String email,
        String displayName
) {}
