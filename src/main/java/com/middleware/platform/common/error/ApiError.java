package com.middleware.platform.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int errorCode,
        String error,
        String message,
        String requestId,
        List<FieldError> fieldErrors,
        /** NFIQ 2 score (0–100) when a fingerprint image was rejected for quality and the platform shares scores. */
        Integer imageQuality
) {
    public ApiError(Instant timestamp, int errorCode, String error, String message, String requestId,
                    List<FieldError> fieldErrors) {
        this(timestamp, errorCode, error, message, requestId, fieldErrors, null);
    }

    public record FieldError(String field, String message) {}
}
