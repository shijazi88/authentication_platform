package com.middleware.platform.gateway.imagecheck;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import lombok.Getter;

/**
 * 400 / 1003 (quality) or 1004 (format) for a fingerprint image the platform
 * refused. The message is the plain bank-facing sentence from the settings;
 * {@code imageQuality} (NFIQ 2 score, may be null) is added to the error body
 * when the platform shares scores.
 */
@Getter
public class ImageRejectedException extends ApplicationException {
    private final Integer imageQuality;

    public ImageRejectedException(ErrorCode code, String bankMessage, Integer imageQuality) {
        super(code, bankMessage);
        this.imageQuality = imageQuality;
    }

    /** 1004 for container/encoding problems, 1003 for everything the operator can fix by re-capturing. */
    public static ErrorCode codeFor(ImageValidationResult.Reason reason) {
        return reason == ImageValidationResult.Reason.FORMAT ? ErrorCode.IMAGE_FORMAT_REJECTED : ErrorCode.IMAGE_QUALITY_REJECTED;
    }
}
