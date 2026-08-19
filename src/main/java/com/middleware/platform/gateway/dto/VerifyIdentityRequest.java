package com.middleware.platform.gateway.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Canonical, provider-agnostic verify request exposed to banks at
 * {@code POST /api/v1/verify/identity}.
 *
 * <p>Two request shapes are accepted (dual-accept during migration):
 * <ul>
 *   <li><b>Encrypted (preferred):</b> {@code encryptedPayload} carries a JWE
 *       (RSA-OAEP-256 + A256GCM) whose plaintext is the JSON
 *       {@code {nationalNumber, biometrics:{fingerPosition, image}}}.</li>
 *   <li><b>Legacy:</b> {@code nationalNumber} + {@code biometrics} in clear —
 *       only permitted while {@code platform.crypto.require-encrypted-pii=false}.</li>
 * </ul>
 * Validation of the decrypted/legacy fields happens in the controller so both
 * shapes share one path.
 */
public record VerifyIdentityRequest(
        @Size(max = 32) String nationalNumber,
        @Valid Biometrics biometrics,
        Exception exception,
        String encryptedPayload
) {
    public record Biometrics(
            @Min(1) @Max(10) Integer fingerPosition,
            String image
    ) {}

    /**
     * Fingerprint-exception marker: when present, the request is verified WITHOUT
     * a fingerprint (person physically cannot provide one). Biometrics are ignored
     * and never sent to the backend. {@code reason} must be a valid
     * {@link BiometricExceptionReason}.
     */
    public record Exception(String reason, String note) {}

    public boolean isEncrypted() {
        return encryptedPayload != null && !encryptedPayload.isBlank();
    }
}
