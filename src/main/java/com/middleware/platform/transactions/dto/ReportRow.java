package com.middleware.platform.transactions.dto;

/**
 * Single row in a daily / monthly transaction report.
 *
 * @param period       day ({@code YYYY-MM-DD}) or month ({@code YYYY-MM})
 * @param total        total transaction count for the bucket
 * @param successCount transactions with status SUCCESS
 * @param failedCount  transactions with status FAILED, TIMEOUT, or REJECTED
 * @param amountMinor  sum of {@code unit_price_minor} for billable rows (minor units)
 * @param currency     ISO-4217 currency code (or empty string if no billable rows)
 */
public record ReportRow(
        String period,
        long total,
        long successCount,
        long failedCount,
        long amountMinor,
        String currency
) {}
