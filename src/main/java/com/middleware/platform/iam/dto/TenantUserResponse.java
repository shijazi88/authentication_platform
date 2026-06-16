package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.TenantUser;

import java.time.Instant;
import java.util.UUID;

public record TenantUserResponse(
        UUID id,
        UUID tenantId,
        String email,
        String displayName,
        boolean active,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static TenantUserResponse from(TenantUser u) {
        return new TenantUserResponse(
                u.getId(), u.getTenantId(), u.getEmail(), u.getDisplayName(),
                u.isActive(), u.getCreatedAt(), u.getLastLoginAt());
    }
}
