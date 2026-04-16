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
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}
}
