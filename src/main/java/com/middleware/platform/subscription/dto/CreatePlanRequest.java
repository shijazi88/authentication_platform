package com.middleware.platform.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreatePlanRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1024) String description,
        @PositiveOrZero long baseFeeMinor,
        @NotBlank @Size(min = 3, max = 3) String currency
) {}
