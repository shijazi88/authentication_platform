package com.middleware.platform.gateway.imagecheck;

/** Outcome of applying {@link ImageValidationSettings} to an {@link ImageInfo}. */
public record ImageValidationResult(Status status, String message, ImageInfo info, Integer nfiq2Score, Reason reason) {

    public ImageValidationResult(Status status, String message, ImageInfo info) {
        this(status, message, info, null, message == null ? Reason.NONE : Reason.FORMAT);
    }

    public ImageValidationResult(Status status, String message, ImageInfo info, Integer nfiq2Score) {
        this(status, message, info, nfiq2Score, message == null ? Reason.NONE : Reason.QUALITY);
    }

    public enum Status {
        /** All rules passed. */
        PASS,
        /** A rule failed but the settings are in WARN mode — recorded, call continues. */
        WARN,
        /** A rule failed in ENFORCE mode — the call is rejected with 400 / 1002. */
        FAIL,
        /** Validation disabled — inspected for reporting only. */
        SKIP
    }

    /**
     * Why the image was flagged. Decides which plain-language sentence the bank
     * receives; the technical detail in {@link #message()} stays on the
     * transaction for our own staff.
     */
    public enum Reason {
        NONE,
        /** Container, encoding, size, resolution — the bank's integration must change. */
        FORMAT,
        /** Blank, partial, smudged, low NFIQ 2 — the operator must re-capture. */
        QUALITY,
        /** The quality service itself could not be reached (warn-only when fail-open). */
        SERVICE
    }

    public boolean rejected() { return status == Status.FAIL; }
}
