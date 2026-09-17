package com.middleware.platform.gateway.imagecheck;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
        @DecimalMin("1.0") double wsqMaxCompressionRatio,
        /** Reject captures with too little ridge texture (partial / smudged / mostly empty). PNG only. */
        boolean checkCoverage,
        /** Minimum fraction (0–1) of 16×16 blocks that contain ridge texture. */
        @DecimalMin("0.0") double minForegroundRatio,
        /** Measure the NIST NFIQ 2 quality score (0–100) with the quality sidecar. */
        boolean checkNfiq2,
        /** Minimum acceptable NFIQ 2 score (ICD recommends 40). */
        @Min(0) @Max(100) int minNfiq2,
        /** When the quality service is unreachable: true = let the call through with a warning, false = reject. */
        boolean nfiq2FailOpen,
        /** Action for a low NFIQ 2 score on its own: ENFORCE rejects, WARN only records the score (structural rules keep {@link #mode}). */
        Mode nfiq2Mode,
        /** Per-call timeout for the quality service, milliseconds. */
        @Min(500) @Max(30_000) int nfiq2TimeoutMs,
        /** Sentence returned to the bank when the capture is poor (blank, partial, low score). No technical detail. */
        @Size(max = 300) String qualityMessage,
        /** Sentence returned to the bank when the image container/encoding/size is not acceptable. */
        @Size(max = 300) String formatMessage,
        /** Include {@code imageQuality} (the NFIQ 2 score) in the API response so the bank's app can guide the operator. */
        boolean returnScoreToBank
) {
    public static final String DEFAULT_QUALITY_MESSAGE =
            "Fingerprint image quality is not good. Please re-capture the fingerprint.";
    public static final String DEFAULT_FORMAT_MESSAGE =
            "Fingerprint image format is not accepted. Please check the image requirements in the integration guide.";

    public static final String KEY = "IMAGE_VALIDATION";

    public enum Mode { ENFORCE, WARN }

    /** Shipped defaults: mirrors ICD §4.2.3, in WARN mode until an admin enforces. */
    public static ImageValidationSettings defaults() {
        return new ImageValidationSettings(
                true, Mode.WARN, Set.of(ImageFormat.WSQ, ImageFormat.PNG),
                200, 200, 2_000, 2_000,
                false, 490, 510,
                2 * 1024 * 1024, true,
                true, 10.0, 15.0,
                true, 0.25,
                true, 40, true,
                Mode.ENFORCE, 5_000, DEFAULT_QUALITY_MESSAGE, DEFAULT_FORMAT_MESSAGE, true);
    }

    /**
     * Fills fields that a document saved by an older release does not carry
     * (Jackson leaves them null / 0), so the verify path never sees a half-built
     * settings object.
     */
    public ImageValidationSettings normalized() {
        ImageValidationSettings d = defaults();
        return new ImageValidationSettings(enabled, mode == null ? d.mode() : mode,
                allowedFormats == null ? d.allowedFormats() : allowedFormats,
                minWidth, minHeight, maxWidth, maxHeight, requirePpi, ppiMin, ppiMax, maxImageBytes,
                requireGrayscale8Bit, checkBlank, minStdDev, wsqMaxCompressionRatio, checkCoverage, minForegroundRatio,
                checkNfiq2, minNfiq2, nfiq2FailOpen,
                nfiq2Mode == null ? d.nfiq2Mode() : nfiq2Mode,
                nfiq2TimeoutMs <= 0 ? d.nfiq2TimeoutMs() : nfiq2TimeoutMs,
                qualityMessage == null || qualityMessage.isBlank() ? d.qualityMessage() : qualityMessage.trim(),
                formatMessage == null || formatMessage.isBlank() ? d.formatMessage() : formatMessage.trim(),
                returnScoreToBank);
    }

    /** Plain sentence for the bank, by reason. */
    public String bankMessage(ImageValidationResult.Reason reason) {
        return reason == ImageValidationResult.Reason.FORMAT ? formatMessage : qualityMessage;
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
