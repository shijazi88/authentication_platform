package com.middleware.platform.transactions.service;

import com.middleware.platform.common.error.BankFacingMessages;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.iam.domain.ApiCredential;
import com.middleware.platform.iam.repo.ApiCredentialRepository;
import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.transactions.domain.Transaction;
import com.middleware.platform.transactions.domain.TransactionStatus;
import com.middleware.platform.transactions.dto.ImageQualityRow;
import com.middleware.platform.transactions.dto.ReportDetailRow;
import com.middleware.platform.transactions.dto.ReportRow;
import com.middleware.platform.transactions.dto.ReportSummary;
import com.middleware.platform.transactions.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Aggregates transactions into daily / monthly report rows + KPI totals, and
 * lists them one by one for the detailed report.
 *
 * <p>Date ranges are inclusive of both {@code from} and {@code to} (whole UTC
 * days), matching the admin Transactions page.
 *
 * <p>The optional {@code statusFilter} parameter ("ALL", "SUCCESS", "FAILED")
 * narrows the report to only the matching status bucket. For the summary the
 * SQL returns all columns for every bucket and the filter trims the totals in
 * Java after the group-by aggregation; breakdowns and details filter in SQL.
 */
@Service
@RequiredArgsConstructor
public class ReportsService {

    private static final List<TransactionStatus> FAILED_STATUSES =
            List.of(TransactionStatus.FAILED, TransactionStatus.TIMEOUT, TransactionStatus.REJECTED);
    private static final int EXPORT_CHUNK = 1000;

    private final TransactionRepository transactionRepository;
    private final ReportPdfExporter pdfExporter;
    private final TenantRepository tenantRepository;
    private final ApiCredentialRepository credentialRepository;

    @Transactional(readOnly = true)
    public ReportSummary daily(UUID tenantId, LocalDate from, LocalDate to, String statusFilter) {
        return build("daily", tenantId, from, to, statusFilter,
                transactionRepository.dailyReportRaw(tenantId.toString(), start(from), end(to)));
    }

    @Transactional(readOnly = true)
    public ReportSummary monthly(UUID tenantId, LocalDate from, LocalDate to, String statusFilter) {
        return build("monthly", tenantId, from, to, statusFilter,
                transactionRepository.monthlyReportRaw(tenantId.toString(), start(from), end(to)));
    }

    /** Fingerprint-quality figures per bank for the admin Reports page. */
    @Transactional(readOnly = true)
    public List<ImageQualityRow> imageQuality(LocalDate from, LocalDate to) {
        Map<String, String> names = new HashMap<>();
        tenantRepository.findAll().forEach(t -> names.put(t.getId().toString(), t.getLegalName()));
        List<ImageQualityRow> rows = new ArrayList<>();
        for (Object[] r : transactionRepository.imageQualityByTenantRaw(start(from), end(to))) {
            String tid = String.valueOf(r[0]);
            long images = num(r[1]).longValue();
            long rejected = num(r[5]).longValue();
            rows.add(new ImageQualityRow(
                    UUID.fromString(tid), names.getOrDefault(tid, tid),
                    images, num(r[2]).longValue(),
                    r[3] == null ? null : Math.round(num(r[3]).doubleValue() * 10) / 10.0,
                    r[4] == null ? null : num(r[4]).intValue(),
                    rejected, num(r[6]).longValue(),
                    images == 0 ? 0.0 : (double) rejected / images,
                    new long[]{num(r[7]).longValue(), num(r[8]).longValue(), num(r[9]).longValue(),
                            num(r[10]).longValue(), num(r[11]).longValue()}));
        }
        return rows;
    }

    /** One page of the detailed report, oldest transaction first. */
    @Transactional(readOnly = true)
    public Page<ReportDetailRow> details(UUID tenantId, LocalDate from, LocalDate to,
                                         String statusFilter, int page, int size) {
        Page<Transaction> txs = transactionRepository.reportDetails(tenantId, statuses(statusFilter),
                start(from), end(to), PageRequest.of(page, size));
        Map<UUID, String> apiKeys = apiKeys(txs.getContent(), new HashMap<>());
        return txs.map(tx -> toDetailRow(tx, apiKeys));
    }

    private static Number num(Object o) { return o == null ? 0 : (Number) o; }

    public void exportDailyCsv(UUID tenantId, LocalDate from, LocalDate to,
                               String statusFilter, OutputStream out) {
        writeCsv(daily(tenantId, from, to, statusFilter), out);
    }

    public void exportMonthlyCsv(UUID tenantId, LocalDate from, LocalDate to,
                                 String statusFilter, OutputStream out) {
        writeCsv(monthly(tenantId, from, to, statusFilter), out);
    }

    public void exportDailyPdf(UUID tenantId, String tenantName, LocalDate from, LocalDate to,
                               String statusFilter, String lang, OutputStream out) {
        pdfExporter.export(daily(tenantId, from, to, statusFilter), tenantName, statusFilter, lang, out);
    }

    public void exportMonthlyPdf(UUID tenantId, String tenantName, LocalDate from, LocalDate to,
                                 String statusFilter, String lang, OutputStream out) {
        pdfExporter.export(monthly(tenantId, from, to, statusFilter), tenantName, statusFilter, lang, out);
    }

    @Transactional(readOnly = true)
    public void exportDetailsCsv(UUID tenantId, LocalDate from, LocalDate to,
                                 String statusFilter, OutputStream out) {
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), false)) {
            w.write('﻿');
            w.println("created_at_utc,transaction_id,type,status,verdict,error_code,error,error_message,"
                    + "exception_reason,exception_note,device_id,device_registered,image_format,nfiq2,"
                    + "image_check,latency_ms,billable,amount_minor,currency,api_key,provider_ref");
            forEachDetailChunk(tenantId, from, to, statusFilter, rows -> {
                for (ReportDetailRow r : rows) {
                    w.println(String.join(",",
                            csv(r.createdAt()), csv(r.transactionId()), csv(r.type()), csv(r.status()),
                            csv(r.verdict()), csv(r.errorCode()), csv(r.error()), csv(r.errorMessage()),
                            csv(r.exceptionReason()), csv(r.exceptionNote()), csv(r.deviceId()),
                            csv(r.deviceRegistered()), csv(r.imageFormat()), csv(r.imageNfiq2()),
                            csv(r.imageCheck()), csv(r.latencyMs()), csv(r.billable()), csv(r.amountMinor()),
                            csv(r.currency()), csv(r.apiKey()), csv(r.providerRef())));
                }
            });
            w.flush();
        }
    }

    @Transactional(readOnly = true)
    public void exportDetailsPdf(UUID tenantId, String tenantName, LocalDate from, LocalDate to,
                                 String statusFilter, String lang, OutputStream out) {
        ReportSummary.Totals totals = daily(tenantId, from, to, statusFilter).totals();
        try (ReportPdfExporter.DetailsWriter pdf =
                     pdfExporter.openDetails(tenantName, from, to, statusFilter, totals, lang, out)) {
            forEachDetailChunk(tenantId, from, to, statusFilter, pdf::addRows);
        }
    }

    // ---------------------------------------------------------------------

    /** Start of {@code from} (UTC) — inclusive lower bound. */
    private static Instant start(LocalDate from) {
        return from.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** Start of the day after {@code to} (UTC) — exclusive upper bound, so {@code to} itself is included. */
    private static Instant end(LocalDate to) {
        return to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static boolean showSuccess(String statusFilter) {
        return "ALL".equalsIgnoreCase(statusFilter) || "SUCCESS".equalsIgnoreCase(statusFilter);
    }

    private static boolean showFailed(String statusFilter) {
        return "ALL".equalsIgnoreCase(statusFilter) || "FAILED".equalsIgnoreCase(statusFilter);
    }

    /** Statuses the filter selects. In-flight (INITIATED) rows are never reported. */
    private static List<TransactionStatus> statuses(String statusFilter) {
        List<TransactionStatus> s = new ArrayList<>();
        if (showSuccess(statusFilter)) s.add(TransactionStatus.SUCCESS);
        if (showFailed(statusFilter)) s.addAll(FAILED_STATUSES);
        return s;
    }

    private void forEachDetailChunk(UUID tenantId, LocalDate from, LocalDate to, String statusFilter,
                                    Consumer<List<ReportDetailRow>> sink) {
        List<TransactionStatus> statuses = statuses(statusFilter);
        Map<UUID, String> apiKeys = new HashMap<>();
        Page<Transaction> page;
        int n = 0;
        do {
            page = transactionRepository.reportDetails(tenantId, statuses, start(from), end(to),
                    PageRequest.of(n++, EXPORT_CHUNK));
            apiKeys(page.getContent(), apiKeys);
            sink.accept(page.getContent().stream().map(tx -> toDetailRow(tx, apiKeys)).toList());
        } while (page.hasNext());
    }

    /** Adds the client IDs of any credentials in {@code txs} not yet in {@code cache}; returns the cache. */
    private Map<UUID, String> apiKeys(List<Transaction> txs, Map<UUID, String> cache) {
        Set<UUID> missing = new HashSet<>();
        for (Transaction tx : txs) {
            if (tx.getCredentialId() != null && !cache.containsKey(tx.getCredentialId())) {
                missing.add(tx.getCredentialId());
            }
        }
        if (!missing.isEmpty()) {
            for (ApiCredential c : credentialRepository.findAllById(missing)) {
                cache.put(c.getId(), c.getClientId());
            }
        }
        return cache;
    }

    private static ReportDetailRow toDetailRow(Transaction tx, Map<UUID, String> apiKeys) {
        ErrorCode ec = ErrorCode.fromCode(tx.getErrorCode());
        String message = tx.getErrorCode() == null ? null : BankFacingMessages.forBank(ec, tx.getErrorMessage());
        return new ReportDetailRow(
                tx.getCreatedAt(), tx.getId(),
                tx.isException() ? "EXCEPTION" : "FINGERPRINT",
                tx.getStatus().name(), tx.getVerdict(),
                tx.getErrorCode(), ec == null ? null : ec.name(), message,
                tx.getExceptionReason(), tx.getExceptionNote(),
                tx.getDeviceId(), tx.getDeviceRegistered(),
                tx.getImageFormat(), tx.getImageNfiq2(), tx.getImageCheck(),
                tx.getLatencyMs(), tx.isBillable(),
                tx.isBillable() ? tx.getUnitPriceMinor() : null, tx.getCurrency(),
                apiKeys.get(tx.getCredentialId()), tx.getProviderRequestId());
    }

    private ReportSummary build(String groupBy, UUID tenantId, LocalDate from, LocalDate to,
                                String statusFilter, List<Object[]> raw) {
        boolean showSuccess = showSuccess(statusFilter);
        boolean showFailed  = showFailed(statusFilter);

        List<ReportRow> rows = new ArrayList<>(raw.size());
        long totalTx = 0, totalSuccess = 0, totalFailed = 0, totalAmount = 0;
        String currency = "";
        for (Object[] r : raw) {
            String period = (String) r[0];
            long success = ((Number) r[2]).longValue();
            long failed = ((Number) r[3]).longValue();
            long amount = ((Number) r[4]).longValue();
            String cur = r[5] == null ? "" : (String) r[5];

            long rowSuccess = showSuccess ? success : 0;
            long rowFailed  = showFailed ? failed : 0;
            long rowTotal   = rowSuccess + rowFailed;
            long rowAmount  = showSuccess ? amount : 0;

            if (rowTotal == 0) continue; // skip empty rows after filter

            rows.add(new ReportRow(period, rowTotal, rowSuccess, rowFailed, rowAmount, cur));
            totalTx += rowTotal;
            totalSuccess += rowSuccess;
            totalFailed += rowFailed;
            totalAmount += rowAmount;
            if (currency.isEmpty() && !cur.isEmpty()) currency = cur;
        }
        double successRate = totalTx == 0 ? 0.0 : (double) totalSuccess / totalTx;
        var totals = new ReportSummary.Totals(
                totalTx, totalSuccess, totalFailed, totalAmount, currency, successRate);
        return new ReportSummary(groupBy, from.toString(), to.toString(), rows, totals,
                breakdown(tenantId, from, to, statusFilter));
    }

    private ReportSummary.Breakdown breakdown(UUID tenantId, LocalDate from, LocalDate to, String statusFilter) {
        String tid = tenantId.toString();
        List<ReportSummary.VerdictCount> verdicts = new ArrayList<>();
        if (showSuccess(statusFilter)) {
            for (Object[] r : transactionRepository.verdictBreakdownRaw(tid, start(from), end(to))) {
                verdicts.add(new ReportSummary.VerdictCount((String) r[0], num(r[1]).longValue()));
            }
        }
        List<ReportSummary.FailureReason> reasons = new ArrayList<>();
        if (showFailed(statusFilter)) {
            for (Object[] r : transactionRepository.failureReasonsRaw(tid, start(from), end(to))) {
                Integer code = r[0] == null ? null : num(r[0]).intValue();
                ErrorCode ec = ErrorCode.fromCode(code);
                // The code's generic bank-facing text describes the whole group; a stored
                // message is per-transaction (e.g. one image's score), so it is only a fallback.
                reasons.add(new ReportSummary.FailureReason(code, ec == null ? null : ec.name(),
                        ec == null ? (String) r[2] : BankFacingMessages.forBank(ec, null),
                        num(r[1]).longValue()));
            }
        }
        List<String> statusNames = statuses(statusFilter).stream().map(Enum::name).toList();
        List<Object[]> perf = transactionRepository.performanceRaw(tid, statusNames, start(from), end(to));
        Object[] p = perf.isEmpty() ? new Object[4] : perf.get(0);
        return new ReportSummary.Breakdown(verdicts, reasons,
                p[0] == null ? null : Math.round(num(p[0]).doubleValue()),
                p[1] == null ? null : num(p[1]).longValue(),
                num(p[2]).longValue(), num(p[3]).longValue());
    }

    /** One CSV field: quoted when needed, and guarded against spreadsheet formula injection. */
    private static String csv(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !(value instanceof Number)) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private void writeCsv(ReportSummary report, OutputStream out) {
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), false)) {
            w.write('﻿');
            w.println("period,total,success,failed,amount_minor,currency");
            for (ReportRow row : report.rows()) {
                w.printf("%s,%d,%d,%d,%d,%s%n",
                        row.period(), row.total(), row.successCount(),
                        row.failedCount(), row.amountMinor(), row.currency());
            }
            w.printf("TOTAL,%d,%d,%d,%d,%s%n",
                    report.totals().totalTransactions(),
                    report.totals().successCount(),
                    report.totals().failedCount(),
                    report.totals().amountMinor(),
                    report.totals().currency());
            w.flush();
        }
    }
}
