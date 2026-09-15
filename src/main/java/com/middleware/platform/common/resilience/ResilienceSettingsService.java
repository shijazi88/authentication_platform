package com.middleware.platform.common.resilience;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.settings.PlatformSettingsService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Applies {@link ResilienceSettings} to the live resilience4j registries. The
 * Spring aspects look the breaker/retry up by name on every call, so replacing
 * the registry entry takes effect immediately.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilienceSettingsService {

    /** Connector instance names carrying @CircuitBreaker / @Retry. */
    public static final List<String> CONNECTORS = List.of("moi-yemen-id", "yemen-id");

    private final PlatformSettingsService settings;
    private final CircuitBreakerRegistry circuitBreakers;
    private final RetryRegistry retries;

    public ResilienceSettings current() {
        return settings.get(ResilienceSettings.KEY, ResilienceSettings.class, ResilienceSettings::defaults);
    }

    /** Re-applies whatever was saved (or the defaults) after every boot. */
    @EventListener(ApplicationReadyEvent.class)
    public void applyOnStartup() {
        apply(current());
    }

    public ResilienceSettings save(ResilienceSettings s, String updatedBy) {
        ResilienceSettings saved = settings.put(ResilienceSettings.KEY, s, updatedBy);
        apply(saved);
        return saved;
    }

    public void apply(ResilienceSettings s) {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(s.slidingWindowSize())
                .minimumNumberOfCalls(Math.min(s.minimumNumberOfCalls(), s.slidingWindowSize()))
                .failureRateThreshold(s.failureRateThresholdPercent())
                .waitDurationInOpenState(Duration.ofSeconds(s.waitDurationOpenSeconds()))
                .permittedNumberOfCallsInHalfOpenState(s.permittedCallsInHalfOpen())
                // Ignored exceptions count neither as failure nor as success, so a run of
                // "not found" answers can neither open the breaker nor mask real outages.
                .ignoreException(t -> s.countOnlyServiceFailures() && !isServiceFailure(t))
                .build();
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(s.retryEnabled() ? s.retryMaxAttempts() : 1)
                .waitDuration(Duration.ofMillis(s.retryWaitMs()))
                .retryExceptions(IOException.class, TimeoutException.class)
                .ignoreExceptions(ApplicationException.class)
                .build();
        for (String name : CONNECTORS) {
            // remove + re-create (rather than replace) so this also works when the
            // entry does not exist yet; registry event consumers re-bind metrics.
            circuitBreakers.remove(name);
            CircuitBreaker cb = circuitBreakers.circuitBreaker(name, cbConfig);
            if (!s.circuitBreakerEnabled()) cb.transitionToDisabledState();
            retries.remove(name);
            retries.retry(name, retryConfig);
        }
        log.info("Resilience policy applied: breaker={} window={} min={} threshold={}% open={}s halfOpen={} serviceFailuresOnly={} retry={} attempts={} wait={}ms",
                s.circuitBreakerEnabled(), s.slidingWindowSize(), s.minimumNumberOfCalls(), s.failureRateThresholdPercent(),
                s.waitDurationOpenSeconds(), s.permittedCallsInHalfOpen(), s.countOnlyServiceFailures(),
                s.retryEnabled(), s.retryMaxAttempts(), s.retryWaitMs());
    }

    /** Only outages count: transport/unknown errors and 5xx-class ApplicationExceptions (codes ≥ 2000). */
    static boolean isServiceFailure(Throwable t) {
        if (t instanceof ApplicationException ae) return ae.getErrorCode().code() >= 2000;
        return true;
    }

    /** Live state per connector for the admin page. */
    public List<BreakerStatus> status() {
        return CONNECTORS.stream().map(name -> {
            CircuitBreaker cb = circuitBreakers.circuitBreaker(name);
            CircuitBreaker.Metrics m = cb.getMetrics();
            return new BreakerStatus(name, cb.getState().name(),
                    m.getFailureRate() < 0 ? null : m.getFailureRate(),
                    m.getNumberOfBufferedCalls(), m.getNumberOfFailedCalls(), m.getNumberOfSuccessfulCalls(),
                    m.getNumberOfNotPermittedCalls());
        }).toList();
    }

    /** Manually closes an open/half-open breaker (e.g. after the provider is confirmed back). */
    public List<BreakerStatus> reset(String name) {
        for (String n : CONNECTORS) {
            if (name == null || name.equals(n)) {
                CircuitBreaker cb = circuitBreakers.circuitBreaker(n);
                if (cb.getState() != CircuitBreaker.State.DISABLED) cb.reset();
            }
        }
        return status();
    }

    public record BreakerStatus(String connector, String state, Float failureRatePercent,
                                int bufferedCalls, int failedCalls, int successfulCalls, long notPermittedCalls) {}
}
