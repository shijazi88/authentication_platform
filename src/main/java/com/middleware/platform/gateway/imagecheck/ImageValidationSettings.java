package com.middleware.platform.gateway.imagecheck;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Admin-editable rules applied to {@code biometrics.image} on receipt, before
 * any wallet reserve or backend call. Stored under platform setting
 * {@link #KEY}; see {@link #defaults()} for the shipped values.
 */
public record ImageValidationSettings(
        /** Master switch. Off = the image is inspected for reporting only, never rejected. */
        boolean enabled,
        /** ENFORCE rejects with 400/1002; WARN records the finding and lets the call through. */
        @NotNull Mode mode,
        /** Containers accepted from banks. */
        @NotNull Set<ImageFormat> allowedFormats,
        @Min(1) @Max(10_000) int minWidth,
        @Min(1) @Max(10_000) int minHeight,
        @Min(1) @Max(10_000) int maxWidth,
        @Min(1) @Max(10_000) int maxHeight,
        /** When true an image that does not declare its resolution is rejected. */
        boolean requirePpi,
        @Min(1) @Max(5_000) int ppiMin,
        @Min(1) @Max(5_000) int ppiMax,
        /** Decoded image bytes. */
        @Min(1_024) @Max(20_000_000) int maxImageBytes,
        /** PNG must be 8-bit greyscale (colour type 0). WSQ is always 8-bit grey. */
        boolean requireGrayscale8Bit,
        /** Reject near-uniform images (PNG only — needs pixel access). */
        boolean checkBlank,
        /** Minimum grey-level standard deviation (0–255 scale) for a non-blank image. */
        @DecimalMin("0.0") double minStdDev,
        /** Maximum WSQ compression ratio (raw pixels ÷ file bytes). */
        @DecimalMin("1.0") double wsqMaxCompressionRatio
) {
    public static final String KEY = "IMAGE_VALIDATION";

    public enum Mode { ENFORCE, WARN }

    /** Shipped defaults: mirrors ICD §4.2.3, in WARN mode until an admin enforces. */
    public static ImageValidationSettings defaults() {
        return new ImageValidationSettings(
                true, Mode.WARN, Set.of(ImageFormat.WSQ, ImageFormat.PNG),
                200, 200, 2_000, 2_000,
                false, 490, 510,
                2 * 1024 * 1024, true,
                true, 10.0, 15.0);
    }

    /** Cross-field sanity; returns a message or null. */
    public String validationError() {
        if (allowedFormats == null || allowedFormats.isEmpty() || allowedFormats.contains(ImageFormat.UNKNOWN))
            return "allowedFormats must contain at least one of WSQ, PNG";
        if (minWidth > maxWidth || minHeight > maxHeight) return "minimum dimensions must not exceed maximum dimensions";
        if (ppiMin > ppiMax) return "ppiMin must not exceed ppiMax";
        return null;
    }
}
