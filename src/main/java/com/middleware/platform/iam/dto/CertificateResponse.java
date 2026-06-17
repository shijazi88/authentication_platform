package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.TenantEncryptionKey;

import java.time.Instant;

/**
 * The tenant's active encryption certificate, handed to clients so they can
 * encrypt the verification PII (JWE: RSA-OAEP-256 + A256GCM). Public material
 * only — never the private key.
 */
public record CertificateResponse(
        String kid,
        String algorithm,
        String encryption,
        String certificatePem,
        String fingerprintSha256,
        Instant expiresAt
) {
    public static CertificateResponse of(TenantEncryptionKey key, String fingerprint) {
        return new CertificateResponse(
                key.getKid(),
                key.getAlgorithm(),
                "A256GCM",
                key.getPublicCertPem(),
                fingerprint,
                key.getExpiresAt());
    }
}
