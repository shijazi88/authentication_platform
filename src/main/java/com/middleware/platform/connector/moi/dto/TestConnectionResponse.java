package com.middleware.platform.connector.moi.dto;

import java.time.Instant;

/**
 * Result of {@code POST /admin/moi-credentials/test} — a token probe. The
 * token itself is NOT returned (security); only a pass/fail + expiry.
 */
public record TestConnectionResponse(
        boolean ok,
        Instant tokenExpiresAt,
        String errorMessage
) {}
