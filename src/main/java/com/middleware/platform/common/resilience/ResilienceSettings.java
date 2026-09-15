package com.middleware.platform.common.resilience;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Admin-editable protection policy for the identity-provider connectors
 * (circuit breaker + retry). Stored under platform setting {@link #KEY} and
 * applied live to the resilience4j registries — no redeploy.
 */
public record ResilienceSettings(
        /** Master switch for the circuit breaker. Off = every call goes to the provider. */
        boolean circuitBreakerEnabled,
        /** Number of most recent calls the failure rate is computed over. */
        @Min(5) @Max(500) int slidingWindowSize,
        /** Calls needed in the window before the breaker may open (capped at the window size). */
        @Min(1) @Max(500) int minimumNumberOfCalls,
        /** Failure percentage that opens the breaker. */
        @Min(1) @Max(100) int failureRateThresholdPercent,
        /** How long the breaker stays open before letting trial calls through. */
        @Min(1) @Max(3600) int waitDurationOpenSeconds,
        /** Trial calls allowed while half-open. */
        @Min(1) @Max(50) int permittedCallsInHalfOpen,
        /**
         * Count only service failures (transport errors, provider 5xx, timeouts)
         * towards the failure rate — never "not found", "no match" or input
         * rejections, which are correct answers, not outages.
         */
        boolean countOnlyServiceFailures,
        /** Master switch for automatic retry of transient transport failures. */
        boolean retryEnabled,
        /** Total attempts including the first call. */
        @Min(1) @Max(5) int retryMaxAttempts,
        /** Pause between attempts. */
        @Min(100) @Max(10_000) int retryWaitMs
) {
    public static final String KEY = "RESILIENCE";

    /** Matches the shipped application.yml policy, plus the service-failures-only rule. */
    public static ResilienceSettings defaults() {
        return new ResilienceSettings(true, 20, 20, 50, 30, 3, true, true, 3, 500);
    }

    public String validationError() {
        if (minimumNumberOfCalls > slidingWindowSize) return "minimumNumberOfCalls must not exceed slidingWindowSize";
        return null;
    }
}
