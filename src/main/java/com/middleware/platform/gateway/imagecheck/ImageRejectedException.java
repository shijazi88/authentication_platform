package com.middleware.platform.gateway.imagecheck;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import lombok.Getter;

/**
 * 400 / 1002 for a fingerprint image the platform refused. The message is the
 * plain bank-facing sentence from the settings; {@code imageQuality} (NFIQ 2
 * score, may be null) is added to the error body when the platform shares scores.
 */
@Getter
public class ImageRejectedException extends ApplicationException {
    private final Integer imageQuality;

    public ImageRejectedException(String bankMessage, Integer imageQuality) {
        super(ErrorCode.VALIDATION_FAILED, bankMessage);
        this.imageQuality = imageQuality;
    }
}
