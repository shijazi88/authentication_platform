package com.middleware.platform.gateway.imagecheck;

/** Outcome of applying {@link ImageValidationSettings} to an {@link ImageInfo}. */
public record ImageValidationResult(Status status, String message, ImageInfo info) {

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

    public boolean rejected() { return status == Status.FAIL; }
}
