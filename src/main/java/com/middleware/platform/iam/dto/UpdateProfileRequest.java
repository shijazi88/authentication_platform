package com.middleware.platform.iam.dto;

import jakarta.validation.constraints.Size;

/** Self-service profile update for a tenant-portal user. */
public record UpdateProfileRequest(
        @Size(max = 255) String displayName
) {}
