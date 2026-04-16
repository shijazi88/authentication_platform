package com.middleware.platform.iam.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Credential issuance response. The plaintext secret is returned ONCE on creation
 * and is never persisted or retrievable later.
 */
public record CredentialResponse(
        UUID id,
        UUID tenantId,
        String clientId,
        String clientSecret,
        String label,
        Instant createdAt
) {}
