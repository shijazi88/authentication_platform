package com.middleware.platform.connector.moi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Update payload for {@code PUT /admin/moi-credentials}. Any field may be
 * omitted to leave that column unchanged; only non-null non-blank values
 * overwrite the existing row.
 */
public record UpdateMoiCredentialsRequest(
        @Size(max = 512) String authUrl,
        @Size(max = 512) String verifyUrl,
        @Size(max = 255) String username,
        @Size(max = 1024) String password,
        @Size(max = 128)  String domain,
        Boolean active,
        @Min(100) @Max(300_000) Integer connectTimeoutMs,
        @Min(100) @Max(300_000) Integer readTimeoutMs,
        @Min(0)   @Max(3600)    Integer tokenRefreshSkewSec
) {}
