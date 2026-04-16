package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.AdminRole;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        AdminRole role
) {}
