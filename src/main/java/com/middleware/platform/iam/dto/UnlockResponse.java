package com.middleware.platform.iam.dto;

public record UnlockResponse(
        String unlockToken,
        long expiresInSeconds
) {}
