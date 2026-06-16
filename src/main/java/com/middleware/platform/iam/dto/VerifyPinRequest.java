package com.middleware.platform.iam.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyPinRequest(
        @NotBlank String pin
) {}
