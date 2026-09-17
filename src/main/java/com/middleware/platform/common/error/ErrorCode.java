package com.middleware.platform.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 4xx
    BAD_REQUEST(1001, HttpStatus.BAD_REQUEST, "Bad request"),
    VALIDATION_FAILED(1002, HttpStatus.BAD_REQUEST, "Validation failed"),
    // Fingerprint image refused by the platform before any charge or provider call (ICD §4.2.3).
    // 1003: poor capture → the operator re-captures. 1004: wrong container/encoding → the bank's integration must change.
    IMAGE_QUALITY_REJECTED(1003, HttpStatus.BAD_REQUEST, "Fingerprint image quality is not good. Please re-capture the fingerprint."),
    IMAGE_FORMAT_REJECTED(1004, HttpStatus.BAD_REQUEST, "Fingerprint image format is not accepted. Please check the image requirements in the integration guide."),
    UNAUTHENTICATED(1101, HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_CREDENTIALS(1102, HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    FORBIDDEN(1201, HttpStatus.FORBIDDEN, "Access denied"),
    ENTITLEMENT_DENIED(1202, HttpStatus.FORBIDDEN, "Subscription does not entitle this operation"),
    PIN_UNLOCK_REQUIRED(1203, HttpStatus.LOCKED, "PIN unlock required"),
    INVALID_PIN(1204, HttpStatus.UNAUTHORIZED, "Invalid PIN"),
    NOT_FOUND(1301, HttpStatus.NOT_FOUND, "Resource not found"),
    BIOMETRIC_NO_MATCH(1302, HttpStatus.UNPROCESSABLE_ENTITY, "Fingerprint did not match the national ID"),
    CONFLICT(1401, HttpStatus.CONFLICT, "Resource conflict"),
    QUOTA_EXCEEDED(1402, HttpStatus.TOO_MANY_REQUESTS, "Quota exceeded"),
    INSUFFICIENT_FUNDS(1403, HttpStatus.PAYMENT_REQUIRED, "Insufficient wallet balance"),

    // 5xx
    INTERNAL_ERROR(2001, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    // Connector failures (2101–2103) all answer 503, never 502/504: Cloudflare
    // replaces an origin 502/504 with its own "error code: 502" page, so the
    // bank would never see errorCode/message. 503 is passed through untouched.
    CONNECTOR_ERROR(2101, HttpStatus.SERVICE_UNAVAILABLE, "Backend connector error"),
    CONNECTOR_TIMEOUT(2102, HttpStatus.SERVICE_UNAVAILABLE, "Backend connector timed out"),
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
