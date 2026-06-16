package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.ApiCredential;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-facing view of an API credential. Never includes the secret — it is
 * BCrypt-hashed and only returned once, at creation.
 */
public record CredentialView(
        UUID id,
        String clientId,
        String label,
        String ipAllowlist,
        boolean active,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt
) {
    public static CredentialView from(ApiCredential c) {
        return new CredentialView(
                c.getId(), c.getClientId(), c.getLabel(), c.getIpAllowlist(),
                c.isActive(), c.getCreatedAt(), c.getLastUsedAt(), c.getExpiresAt());
    }
}
