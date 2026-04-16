package com.middleware.platform.iam.security;

import java.util.UUID;

public record TenantPrincipal(UUID tenantId, String tenantCode, UUID credentialId, String clientId) {}
