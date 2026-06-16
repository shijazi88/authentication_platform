package com.middleware.platform.iam.dto;

import java.util.UUID;

public record TenantMeResponse(
        String email,
        String displayName,
        UUID tenantId,
        String tenantCode,
        String tenantName
) {}
