package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.AdminRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAdminUserRequest(
        @Size(max = 255) String displayName,
        @NotNull AdminRole role
) {}
