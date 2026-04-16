package com.middleware.platform.iam.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Fired by {@code ClientCredentialsAuthFilter} after a successful authentication.
 * An {@code @Async} listener picks it up and updates {@code api_credentials.last_used_at}
 * out-of-band so the auth path stays free of write latency.
 */
public record CredentialUsedEvent(UUID credentialId, Instant usedAt) {}
