package com.middleware.platform.transactions.service;

import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.transactions.dto.ImageQualityRow;
import com.middleware.platform.transactions.dto.ReportRow;
import com.middleware.platform.transactions.dto.ReportSummary;
import com.middleware.platform.transactions.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

/**
 * Aggregates transactions into daily / monthly report rows + KPI totals.
 *
 * <p>The optional {@code statusFilter} parameter ("ALL", "SUCCESS", "FAILED")
 * narrows the report to only the matching status bucket. The SQL returns all
 * columns for every bucket; the filter trims the totals in Java after the
 * group-by aggregation — this keeps the native queries unchanged.
 */
@Service
@RequiredArgsConstructor
public class ReportsService {

    private final TransactionRepository transactionRepository;
    private final ReportPdfExporter pdfExporter;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public ReportSummary daily(UUID tenantId, LocalDate from, LocalDate to, String statusFilter) {
        return build("daily", from, to, statusFilter,
                transactionRepository.dailyReportRaw(
                        tenantId.toString(),
                        from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                        to.atStartOfDay(ZoneOffset.UTC).toInstant()));
    }

    @Transactional(readOnly = true)
    public ReportSummary monthly(UUID tenantId, LocalDate from, LocalDate to, String statusFilter) {
        return build("monthly", from, to, statusFilter,
                transactionRepository.monthlyReportRaw(
                        tenantId.toString(),
                        from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                        to.atStartOfDay(ZoneOffset.UTC).toInstant()));
    }

    /** Fingerprint-quality figures per bank for the admin Reports page. */
    @Transactional(readOnly = true)
    public List<ImageQualityRow> imageQuality(LocalDate from, LocalDate to) {
        Map<String, String> names = new HashMap<>();
        tenantRepository.findAll().forEach(t -> names.put(t.getId().toString(), t.getLegalName()));
        List<ImageQualityRow> rows = new ArrayList<>();
        for (Object[] r : transactionRepository.imageQualityByTenantRaw(
                from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())) {
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
                               String statusFilter, OutputStream out) {
        pdfExporter.export(daily(tenantId, from, to, statusFilter), tenantName, out);
    }

    public void exportMonthlyPdf(UUID tenantId, String tenantName, LocalDate from, LocalDate to,
                                 String statusFilter, OutputStream out) {
        pdfExporter.export(monthly(tenantId, from, to, statusFilter), tenantName, out);
    }

    // ---------------------------------------------------------------------

    private ReportSummary build(String groupBy, LocalDate from, LocalDate to,
                                String statusFilter, List<Object[]> raw) {
        boolean showSuccess = "ALL".equalsIgnoreCase(statusFilter) || "SUCCESS".equalsIgnoreCase(statusFilter);
        boolean showFailed  = "ALL".equalsIgnoreCase(statusFilter) || "FAILED".equalsIgnoreCase(statusFilter);

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
        return new ReportSummary(groupBy, from.toString(), to.toString(), rows, totals);
    }

    private void writeCsv(ReportSummary report, OutputStream out) {
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), false)) {
            w.write('\uFEFF');
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
