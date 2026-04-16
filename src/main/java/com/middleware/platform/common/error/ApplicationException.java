package com.middleware.platform.common.error;

import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {
    private final ErrorCode errorCode;

    public ApplicationException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public ApplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApplicationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static ApplicationException notFound(String what) {
        return new ApplicationException(ErrorCode.NOT_FOUND, what + " not found");
    }

    public static ApplicationException conflict(String message) {
        return new ApplicationException(ErrorCode.CONFLICT, message);
    }

    public static ApplicationException forbidden(String message) {
        return new ApplicationException(ErrorCode.FORBIDDEN, message);
    }
}
