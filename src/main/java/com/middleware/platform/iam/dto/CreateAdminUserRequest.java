package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 255) String displayName,
        @NotNull AdminRole role
) {}
