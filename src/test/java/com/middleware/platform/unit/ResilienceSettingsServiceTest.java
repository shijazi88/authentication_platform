package com.middleware.platform.unit;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.common.resilience.ResilienceSettings;
import com.middleware.platform.common.resilience.ResilienceSettingsService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ResilienceSettingsServiceTest {

    private final CircuitBreakerRegistry cbs = CircuitBreakerRegistry.ofDefaults();
    private final RetryRegistry retries = RetryRegistry.ofDefaults();
    private final ResilienceSettingsService svc = new ResilienceSettingsService(null, cbs, retries);

    @Test
    void appliedConfigIsVisibleOnTheRegistry() {
        svc.apply(new ResilienceSettings(true, 10, 4, 60, 45, 2, true, true, 2, 250));
        CircuitBreaker cb = cbs.circuitBreaker("moi-yemen-id");
        assertThat(cb.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(10);
        assertThat(cb.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(4);
        assertThat(cb.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(60f);
        assertThat(cb.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(2);
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(retries.retry("moi-yemen-id").getRetryConfig().getMaxAttempts()).isEqualTo(2);
        assertThat(cbs.circuitBreaker("yemen-id").getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(10);
    }

    @Test
    void notFoundAndNoMatchDoNotCountAsFailures_butServiceErrorsDo() {
        svc.apply(new ResilienceSettings(true, 10, 4, 50, 30, 3, true, true, 3, 500));
        CircuitBreaker cb = cbs.circuitBreaker("moi-yemen-id");
        for (int i = 0; i < 6; i++) {
            cb.onError(10, TimeUnit.MILLISECONDS, new ApplicationException(ErrorCode.NOT_FOUND, "not found"));
            cb.onError(10, TimeUnit.MILLISECONDS, new ApplicationException(ErrorCode.BIOMETRIC_NO_MATCH, "no match"));
        }
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(cb.getMetrics().getNumberOfFailedCalls()).isZero();

        for (int i = 0; i < 4; i++) {
            cb.onError(10, TimeUnit.MILLISECONDS, new ApplicationException(ErrorCode.CONNECTOR_ERROR, "MOI HTTP 500"));
        }
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(svc.status().get(0).state()).isEqualTo("OPEN");
        assertThat(svc.reset("moi-yemen-id").get(0).state()).isEqualTo("CLOSED");
    }

    @Test
    void disabledBreakerPermitsEverything_andRetryOffMeansOneAttempt() {
        svc.apply(new ResilienceSettings(false, 20, 20, 50, 30, 3, true, false, 3, 500));
        CircuitBreaker cb = cbs.circuitBreaker("moi-yemen-id");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.DISABLED);
        for (int i = 0; i < 50; i++) cb.onError(10, TimeUnit.MILLISECONDS, new RuntimeException("boom"));
        assertThat(cb.tryAcquirePermission()).isTrue();
        assertThat(retries.retry("moi-yemen-id").getRetryConfig().getMaxAttempts()).isEqualTo(1);
        assertThat(new ResilienceSettings(true, 10, 20, 50, 30, 3, true, true, 3, 500).validationError()).contains("minimumNumberOfCalls");
    }
}
