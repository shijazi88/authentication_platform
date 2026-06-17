package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.EncryptionKeyStatus;
import com.middleware.platform.iam.domain.TenantEncryptionKey;

import java.time.Instant;

/** Admin-facing view of a tenant encryption key (no private material). */
public record EncryptionKeyView(
        String kid,
        String algorithm,
        EncryptionKeyStatus status,
        String fingerprintSha256,
        Instant createdAt,
        Instant rotatedAt,
        Instant expiresAt
) {
    public static EncryptionKeyView of(TenantEncryptionKey k, String fingerprint) {
        return new EncryptionKeyView(
                k.getKid(), k.getAlgorithm(), k.getStatus(), fingerprint,
                k.getCreatedAt(), k.getRotatedAt(), k.getExpiresAt());
    }
}
