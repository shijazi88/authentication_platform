package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.Tenant;
import com.middleware.platform.iam.domain.TenantStatus;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String code,
        String legalName,
        String contactEmail,
        TenantStatus status,
        Instant createdAt
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(
                t.getId(),
                t.getCode(),
                t.getLegalName(),
                t.getContactEmail(),
                t.getStatus(),
                t.getCreatedAt()
        );
    }
}
