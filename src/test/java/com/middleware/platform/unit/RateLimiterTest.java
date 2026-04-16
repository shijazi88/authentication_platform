package com.middleware.platform.unit;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.gateway.orchestrator.RateLimiter;
import com.middleware.platform.transactions.repo.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterTest {

    @Mock TransactionRepository transactionRepository;
    @InjectMocks RateLimiter rateLimiter;

    private static final UUID CRED = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OP = UUID.randomUUID();

    @Test
    @DisplayName("check → passes when under rate limit")
    void check_underLimit() {
        assertThatCode(() -> {
            for (int i = 0; i < 5; i++) {
                rateLimiter.check(CRED, TENANT, OP, 10, null);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("check → throws QUOTA_EXCEEDED when rate limit breached")
    void check_rateLimitBreached() {
        assertThatThrownBy(() -> {
            for (int i = 0; i < 6; i++) {
                rateLimiter.check(CRED, TENANT, OP, 5, null);
            }
        }).isInstanceOf(ApplicationException.class)
          .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode())
                  .isEqualTo(ErrorCode.QUOTA_EXCEEDED));
    }

    @Test
    @DisplayName("check → passes when no limits configured (both null)")
    void check_noLimits() {
        assertThatCode(() -> {
            for (int i = 0; i < 1000; i++) {
                rateLimiter.check(CRED, TENANT, OP, null, null);
            }
        }).doesNotThrowAnyException();
    }
}
