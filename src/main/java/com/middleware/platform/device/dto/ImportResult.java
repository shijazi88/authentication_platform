package com.middleware.platform.device.dto;

import java.util.List;

/** Summary of an Excel device import. */
public record ImportResult(
        int created,
        int skipped,
        List<RowError> errors
) {
    public record RowError(int row, String message) {}
}
