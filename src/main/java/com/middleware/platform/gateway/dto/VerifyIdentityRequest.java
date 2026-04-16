package com.middleware.platform.gateway.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Canonical, provider-agnostic verify request exposed to banks at
 * {@code POST /api/v1/verify/identity}.
 *
 * <p>The orchestrator dispatches this to the registered Yemen ID connector
 * for v1; future identity providers can be added without changing this DTO.
 */
public record VerifyIdentityRequest(
        @NotBlank @Size(max = 32) String nationalNumber,
        @Valid Biometrics biometrics
) {
    public record Biometrics(
            @Min(1) @Max(10) Integer fingerPosition,
            @Size(max = 32000) String image
    ) {}
}
