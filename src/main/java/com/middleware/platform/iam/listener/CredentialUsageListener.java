package com.middleware.platform.iam.listener;

import com.middleware.platform.iam.event.CredentialUsedEvent;
import com.middleware.platform.iam.repo.ApiCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Off-the-hot-path updater for {@code api_credentials.last_used_at}.
 *
 * <p>The auth filter publishes a {@link CredentialUsedEvent} after a successful
 * authentication and returns immediately. This listener runs on a separate
 * thread (Spring's default async executor) and issues a single UPDATE in its
 * own transaction.
 *
 * <p>If the database is unavailable the credential's last_used_at silently
 * lags — that's an acceptable trade for keeping every authenticated request
 * free of an extra synchronous DB write.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CredentialUsageListener {

    private final ApiCredentialRepository credentialRepository;

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCredentialUsed(CredentialUsedEvent event) {
        try {
            credentialRepository.findById(event.credentialId()).ifPresent(cred -> {
                cred.setLastUsedAt(event.usedAt());
                // Dirty checking flushes on commit.
            });
        } catch (Exception ex) {
            log.warn("Failed to update last_used_at for credential {}: {}",
                    event.credentialId(), ex.getMessage());
        }
    }
}
