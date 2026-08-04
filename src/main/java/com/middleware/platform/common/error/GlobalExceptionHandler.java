package com.middleware.platform.common.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiError> handleApplication(ApplicationException ex, HttpServletRequest req) {
        ErrorCode ec = ex.getErrorCode();
        if (ec.status().is5xxServerError()) {
            log.error("ApplicationException [{}] {}", ec.name(), ex.getMessage(), ex);
        } else {
            log.warn("ApplicationException [{}] {}", ec.name(), ex.getMessage());
        }
        return build(ec, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Validation failed", req, fieldErrors);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        log.debug("Authentication failed: {}", ex.getMessage());
        return build(ErrorCode.UNAUTHENTICATED, ex.getMessage(), req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(ErrorCode.FORBIDDEN, ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return build(ErrorCode.INTERNAL_ERROR, "Unexpected error", req, null);
    }

    private ResponseEntity<ApiError> build(ErrorCode ec, String message, HttpServletRequest req,
                                           List<ApiError.FieldError> fieldErrors) {
        String requestId = (String) req.getAttribute("X-Request-Id");
        ApiError body = new ApiError(
                Instant.now(),
                ec.code(),
                ec.name(),
                message != null ? message : ec.defaultMessage(),
                requestId,
                fieldErrors
        );
        return ResponseEntity.status(ec.status())
                .header("X-Error-Code", String.valueOf(ec.code()))
                .header("X-Error-Msg", headerSafe(body.message()))
                .header("X-Request-Id", requestId == null ? "" : requestId)
                .body(body);
    }

    /**
     * Make a message safe to put in an HTTP response header.
     *
     * <p>Error messages are not always ours: {@code MoiYemenIdConnector} builds
     * VALIDATION_FAILED from MOI's {@code X-Error-Message} header, so upstream
     * text lands here verbatim. Header values are single-line ISO-8859-1 — a
     * message carrying Arabic, a newline, or a control character produces a
     * malformed response that the upstream proxy rejects, and the caller sees a
     * bare 502 instead of this JSON error (the app stays healthy, so nothing
     * looks wrong server-side). A newline would also allow response splitting
     * from an upstream-controlled value.
     *
     * <p>The header is a convenience mirror; the full, correctly UTF-8 encoded
     * message is always in the JSON body, so nothing is lost by sanitising here.
     */
    private static String headerSafe(String message) {
        if (message == null || message.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(Math.min(message.length(), MAX_HEADER_MSG));
        for (int i = 0; i < message.length() && sb.length() < MAX_HEADER_MSG; i++) {
            char c = message.charAt(i);
            if (c == '\r' || c == '\n' || c == '\t') sb.append(' ');
            else if (c >= 0x20 && c <= 0x7E) sb.append(c);
            else sb.append('?');
        }
        return sb.toString().trim();
    }

    private static final int MAX_HEADER_MSG = 200;
}
