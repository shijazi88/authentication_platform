package com.middleware.platform.transactions.dto;

import java.util.List;

/**
 * Aggregated report response: a collection of per-bucket rows plus totals.
 * The frontend reads {@link #rows} to draw the chart/table and {@link #totals}
 * to populate the KPI cards.
 */
public record ReportSummary(
        String groupBy,        // "daily" | "monthly"
        String from,           // ISO date inclusive
        String to,             // ISO date exclusive
        List<ReportRow> rows,
        Totals totals
) {
    public record Totals(
            long totalTransactions,
            long successCount,
            long failedCount,
            long amountMinor,
            String currency,
            double successRate    // 0.0 .. 1.0
    ) {}
}
