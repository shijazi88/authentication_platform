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
        String encryptedPayload
) {
    public record Biometrics(
            @Min(1) @Max(10) Integer fingerPosition,
            String image
    ) {}

    public boolean isEncrypted() {
        return encryptedPayload != null && !encryptedPayload.isBlank();
    }
}
