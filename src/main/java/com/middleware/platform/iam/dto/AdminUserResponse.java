package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.AdminRole;
import com.middleware.platform.iam.domain.AdminUser;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        AdminRole role,
        boolean active,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static AdminUserResponse from(AdminUser u) {
        return new AdminUserResponse(
                u.getId(),
                u.getEmail(),
                u.getDisplayName(),
                u.getRole(),
                u.isActive(),
                u.getCreatedAt(),
                u.getLastLoginAt()
        );
    }
}
