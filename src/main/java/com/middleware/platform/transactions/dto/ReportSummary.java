package com.middleware.platform.transactions.dto;

import java.util.List;

/**
 * Aggregated report response: a collection of per-bucket rows plus totals.
 * The frontend reads {@link #rows} to draw the chart/table, {@link #totals}
 * to populate the KPI cards and {@link #breakdown} for the detail sections.
 */
public record ReportSummary(
        String groupBy,        // "daily" | "monthly"
        String from,           // ISO date inclusive
        String to,             // ISO date inclusive
        List<ReportRow> rows,
        Totals totals,
        Breakdown breakdown
) {
    public record Totals(
            long totalTransactions,
            long successCount,
            long failedCount,
            long amountMinor,
            String currency,
            double successRate    // 0.0 .. 1.0
    ) {}

    /**
     * What sits behind the totals, honouring the status filter: verdicts are
     * empty when only failures are shown, failure reasons when only successes are.
     *
     * @param verdicts       successful transactions per verdict, most frequent first
     * @param failureReasons failed transactions per error code, most frequent first
     * @param avgLatencyMs   mean response time (null when no rows)
     * @param maxLatencyMs   slowest response time (null when no rows)
     * @param billableCount  transactions charged to the wallet
     * @param exceptionCount fingerprint-exception requests (no fingerprint taken)
     */
    public record Breakdown(
            List<VerdictCount> verdicts,
            List<FailureReason> failureReasons,
            Long avgLatencyMs,
            Long maxLatencyMs,
            long billableCount,
            long exceptionCount
    ) {}

    /** @param verdict MATCH, NO_MATCH, NO_VERIFICATION_POSSIBLE, EXEMPT, or null for rows saved before verdicts were recorded */
    public record VerdictCount(String verdict, long count) {}

    /**
     * @param errorCode numeric response code (null if none was recorded)
     * @param error     error name, e.g. IMAGE_QUALITY_REJECTED
     * @param message   bank-facing description
     */
    public record FailureReason(Integer errorCode, String error, String message, long count) {}
}
