package com.middleware.platform.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetPinRequest(
        @NotBlank @Pattern(regexp = "\\d{4,8}", message = "PIN must be 4–8 digits") String pin,
        // Required only when changing an existing PIN.
        String currentPin
) {}
