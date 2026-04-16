package com.middleware.platform.transactions.repo;

import com.middleware.platform.transactions.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Transaction> findByTenantIdAndCreatedAtBetween(UUID tenantId, Instant from, Instant to,
                                                       Pageable pageable);
    long countByTenantIdAndCreatedAtBetween(UUID tenantId, Instant from, Instant to);

    /**
     * Daily aggregation of transactions for a tenant within a half-open
     * interval [{@code from}, {@code to}). Returns one row per (day, currency)
     * pair so reports can show mixed-currency periods correctly.
     *
     * <p>Result columns:
     * <ol start="0">
     *   <li>day (varchar YYYY-MM-DD)</li>
     *   <li>total transaction count (long)</li>
     *   <li>success count (long)</li>
     *   <li>failed count (long, includes FAILED + TIMEOUT + REJECTED)</li>
     *   <li>billable amount sum, minor units (long)</li>
     *   <li>currency (varchar(3) or empty if no billable rows)</li>
     * </ol>
     */
    @Query(value = """
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS day,
                   COUNT(*) AS total,
                   SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS successCount,
                   SUM(CASE WHEN status IN ('FAILED','TIMEOUT','REJECTED') THEN 1 ELSE 0 END) AS failedCount,
                   COALESCE(SUM(CASE WHEN billable = 1 THEN unit_price_minor ELSE 0 END), 0) AS amountMinor,
                   COALESCE(MAX(currency), '') AS currency
              FROM transactions
             WHERE tenant_id = :tenantId
               AND created_at >= :from
               AND created_at <  :to
             GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
             ORDER BY day
            """, nativeQuery = true)
    List<Object[]> dailyReportRaw(@Param("tenantId") String tenantId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);

    /**
     * Same as {@link #dailyReportRaw} but grouped by calendar month.
     * Day column becomes the period in {@code YYYY-MM} form.
     */
    @Query(value = """
            SELECT DATE_FORMAT(created_at, '%Y-%m') AS period,
                   COUNT(*) AS total,
                   SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS successCount,
                   SUM(CASE WHEN status IN ('FAILED','TIMEOUT','REJECTED') THEN 1 ELSE 0 END) AS failedCount,
                   COALESCE(SUM(CASE WHEN billable = 1 THEN unit_price_minor ELSE 0 END), 0) AS amountMinor,
                   COALESCE(MAX(currency), '') AS currency
              FROM transactions
             WHERE tenant_id = :tenantId
               AND created_at >= :from
               AND created_at <  :to
             GROUP BY DATE_FORMAT(created_at, '%Y-%m')
             ORDER BY period
            """, nativeQuery = true)
    List<Object[]> monthlyReportRaw(@Param("tenantId") String tenantId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);
}
