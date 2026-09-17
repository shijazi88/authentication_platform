package com.middleware.platform.iam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Admin update of a bank's own fingerprint-quality threshold. {@code minNfiq2}
 * null clears the override so the platform default applies again.
 */
public record UpdateQualityRequest(
        @Min(0) @Max(100) Integer minNfiq2
) {}
