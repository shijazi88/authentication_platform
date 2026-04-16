package com.middleware.platform.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 4xx
    BAD_REQUEST(1001, HttpStatus.BAD_REQUEST, "Bad request"),
    VALIDATION_FAILED(1002, HttpStatus.BAD_REQUEST, "Validation failed"),
    UNAUTHENTICATED(1101, HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_CREDENTIALS(1102, HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    FORBIDDEN(1201, HttpStatus.FORBIDDEN, "Access denied"),
    ENTITLEMENT_DENIED(1202, HttpStatus.FORBIDDEN, "Subscription does not entitle this operation"),
    NOT_FOUND(1301, HttpStatus.NOT_FOUND, "Resource not found"),
    CONFLICT(1401, HttpStatus.CONFLICT, "Resource conflict"),
    QUOTA_EXCEEDED(1402, HttpStatus.TOO_MANY_REQUESTS, "Quota exceeded"),

    // 5xx
    INTERNAL_ERROR(2001, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    CONNECTOR_ERROR(2101, HttpStatus.BAD_GATEWAY, "Backend connector error"),
    CONNECTOR_TIMEOUT(2102, HttpStatus.GATEWAY_TIMEOUT, "Backend connector timed out"),
    CONNECTOR_UNAVAILABLE(2103, HttpStatus.SERVICE_UNAVAILABLE, "Backend connector unavailable");

    private final int code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public int code() { return code; }
    public HttpStatus status() { return status; }
    public String defaultMessage() { return defaultMessage; }
}
