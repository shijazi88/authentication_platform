package com.middleware.platform.connector.moi.dto;

import com.middleware.platform.connector.moi.domain.MoiCredentials;

import java.time.Instant;

/** Outbound DTO — the password is never exposed; callers see a fixed mask. */
public record MoiCredentialsResponse(
        String authUrl,
        String verifyUrl,
        String username,
        String passwordMasked,
        String domain,
        boolean active,
        int connectTimeoutMs,
        int readTimeoutMs,
        int tokenRefreshSkewSec,
        String updatedBy,
        Instant updatedAt,
        Instant createdAt
) {
    public static MoiCredentialsResponse from(MoiCredentials c) {
        return new MoiCredentialsResponse(
                c.getAuthUrl(),
                c.getVerifyUrl(),
                c.getUsername(),
                c.getPassword() == null || c.getPassword().isBlank() ? "" : "********",
                c.getDomain(),
                c.isActive(),
                c.getConnectTimeoutMs(),
                c.getReadTimeoutMs(),
                c.getTokenRefreshSkewSec(),
                c.getUpdatedBy(),
                c.getUpdatedAt(),
                c.getCreatedAt()
        );
    }
}
