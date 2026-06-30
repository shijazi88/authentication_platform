package com.middleware.platform.transactions.repo;

import com.middleware.platform.transactions.domain.Transaction;
import com.middleware.platform.transactions.domain.TransactionStatus;
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

    /**
     * Admin transaction listing with optional filters. Any parameter left null
     * is ignored, so this single query serves all filter combinations (client,
     * status, and a [from, to) date range). Sorting/paging come from {@code Pageable}.
     */
    @Query("""
            select t from Transaction t
             where (:tenantId  is null or t.tenantId  = :tenantId)
               and (:status    is null or t.status    = :status)
               and (:errorCode is null or t.errorCode = :errorCode)
               and (:billable  is null or t.billable  = :billable)
               and (:idq       is null or lower(cast(t.id as string)) like lower(concat('%', :idq, '%')))
               and (:from      is null or t.createdAt >= :from)
               and (:to        is null or t.createdAt <  :to)
            """)
    Page<Transaction> filter(@Param("tenantId") UUID tenantId,
                             @Param("status") TransactionStatus status,
                             @Param("errorCode") Integer errorCode,
                             @Param("billable") Boolean billable,
                             @Param("idq") String idq,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
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

    @Query(value = """
            SELECT * FROM transactions
             WHERE LOWER(CAST(id AS CHAR)) LIKE LOWER(CONCAT(:q, '%'))
                OR LOWER(provider_request_id) LIKE LOWER(CONCAT('%', :q, '%'))
             ORDER BY created_at DESC
             LIMIT :limit
            """, nativeQuery = true)
    List<Transaction> search(@Param("q") String q, @Param("limit") int limit);
}
