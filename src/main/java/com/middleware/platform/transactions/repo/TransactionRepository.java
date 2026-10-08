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
               and (:exception is null or t.exception = :exception)
               and (:idq       is null or lower(cast(t.id as string)) like lower(concat('%', :idq, '%')))
               and (:from      is null or t.createdAt >= :from)
               and (:to        is null or t.createdAt <  :to)
            """)
    Page<Transaction> filter(@Param("tenantId") UUID tenantId,
                             @Param("status") TransactionStatus status,
                             @Param("errorCode") Integer errorCode,
                             @Param("billable") Boolean billable,
                             @Param("exception") Boolean exception,
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

    /**
     * Fingerprint-quality figures per tenant. Columns: tenant_id, images, scored,
     * avgNfiq2, minNfiq2, rejected, warned, b0..b4 (NFIQ 2 bands of 20).
     */
    @Query(value = """
            SELECT tenant_id,
                   COUNT(*) AS images,
                   SUM(CASE WHEN image_nfiq2 IS NOT NULL THEN 1 ELSE 0 END) AS scored,
                   AVG(image_nfiq2) AS avgNfiq2,
                   MIN(image_nfiq2) AS minNfiq2,
                   SUM(CASE WHEN image_check = 'FAIL' THEN 1 ELSE 0 END) AS rejected,
                   SUM(CASE WHEN image_check = 'WARN' THEN 1 ELSE 0 END) AS warned,
                   SUM(CASE WHEN image_nfiq2 BETWEEN 0 AND 19 THEN 1 ELSE 0 END) AS b0,
                   SUM(CASE WHEN image_nfiq2 BETWEEN 20 AND 39 THEN 1 ELSE 0 END) AS b1,
                   SUM(CASE WHEN image_nfiq2 BETWEEN 40 AND 59 THEN 1 ELSE 0 END) AS b2,
                   SUM(CASE WHEN image_nfiq2 BETWEEN 60 AND 79 THEN 1 ELSE 0 END) AS b3,
                   SUM(CASE WHEN image_nfiq2 >= 80 THEN 1 ELSE 0 END) AS b4
              FROM transactions
             WHERE image_check IS NOT NULL
               AND created_at >= :from
               AND created_at <  :to
             GROUP BY tenant_id
             ORDER BY images DESC
            """, nativeQuery = true)
    List<Object[]> imageQualityByTenantRaw(@Param("from") Instant from, @Param("to") Instant to);

    /** Successful transactions per verdict for a tenant in [from, to). Columns: verdict (may be null), count. */
    @Query(value = """
            SELECT verdict, COUNT(*) AS cnt
              FROM transactions
             WHERE tenant_id = :tenantId
               AND status = 'SUCCESS'
               AND created_at >= :from
               AND created_at <  :to
             GROUP BY verdict
             ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> verdictBreakdownRaw(@Param("tenantId") String tenantId,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);

    /**
     * Failed transactions per error code for a tenant in [from, to), most frequent first.
     * Columns: error_code (may be null), count, a sample error_message.
     */
    @Query(value = """
            SELECT error_code, COUNT(*) AS cnt, MAX(error_message) AS sample
              FROM transactions
             WHERE tenant_id = :tenantId
               AND status IN ('FAILED','TIMEOUT','REJECTED')
               AND created_at >= :from
               AND created_at <  :to
             GROUP BY error_code
             ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> failureReasonsRaw(@Param("tenantId") String tenantId,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to);

    /**
     * Response-time and billing figures for a tenant in [from, to), limited to the given statuses.
     * Columns: avg latency ms, max latency ms, billable count, exception count.
     */
    @Query(value = """
            SELECT AVG(latency_ms), MAX(latency_ms),
                   COALESCE(SUM(CASE WHEN billable = 1 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN is_exception = 1 THEN 1 ELSE 0 END), 0)
              FROM transactions
             WHERE tenant_id = :tenantId
               AND status IN (:statuses)
               AND created_at >= :from
               AND created_at <  :to
            """, nativeQuery = true)
    List<Object[]> performanceRaw(@Param("tenantId") String tenantId,
                                  @Param("statuses") List<String> statuses,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);

    /** Per-transaction report rows for a tenant in [from, to), oldest first. */
    @Query("""
            select t from Transaction t
             where t.tenantId = :tenantId
               and t.status in :statuses
               and t.createdAt >= :from
               and t.createdAt <  :to
             order by t.createdAt asc
            """)
    Page<Transaction> reportDetails(@Param("tenantId") UUID tenantId,
                                    @Param("statuses") List<TransactionStatus> statuses,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to,
                                    Pageable pageable);

    @Query(value = """
            SELECT * FROM transactions
             WHERE LOWER(CAST(id AS CHAR)) LIKE LOWER(CONCAT(:q, '%'))
                OR LOWER(provider_request_id) LIKE LOWER(CONCAT('%', :q, '%'))
             ORDER BY created_at DESC
             LIMIT :limit
            """, nativeQuery = true)
    List<Transaction> search(@Param("q") String q, @Param("limit") int limit);
}
