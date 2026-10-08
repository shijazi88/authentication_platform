package com.middleware.platform.transactions.api;

import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.transactions.dto.ImageQualityRow;
import com.middleware.platform.transactions.dto.ReportDetailRow;
import com.middleware.platform.transactions.dto.ReportSummary;
import com.middleware.platform.transactions.service.ReportsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Admin-portal report endpoints.
 *
 * <p>All endpoints accept an optional {@code status} filter:
 * {@code ALL} (default), {@code SUCCESS}, {@code FAILED}. Date ranges are
 * inclusive of both {@code from} and {@code to}.
 */
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsService reportsService;
    private final TenantRepository tenantRepository;

    @GetMapping("/transactions/daily")
    public ReportSummary daily(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status) {
        return reportsService.daily(tenantId, from, to, status);
    }

    /** Fingerprint-quality figures per bank (all tenants) for a date range, inclusive. */
    @GetMapping("/image-quality")
    public List<ImageQualityRow> imageQuality(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportsService.imageQuality(from, to);
    }

    @GetMapping("/transactions/monthly")
    public ReportSummary monthly(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status) {
        return reportsService.monthly(tenantId, from, to, status);
    }

    /** Per-transaction report, oldest first — the page preview of the details export. */
    @GetMapping("/transactions/details")
    public Page<ReportDetailRow> details(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return reportsService.details(tenantId, from, to, status, Math.max(0, page), Math.min(Math.max(1, size), 200));
    }

    @GetMapping(value = "/transactions/details/export.csv", produces = "text/csv; charset=UTF-8")
    public void detailsCsv(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws IOException {
        attachCsvHeaders(response, "motabiq-transaction-details-" + from + "-to-" + to + ".csv");
        reportsService.exportDetailsCsv(tenantId, from, to, status, response.getOutputStream());
    }

    @GetMapping(value = "/transactions/details/export.pdf", produces = "application/pdf")
    public void detailsPdf(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws IOException {
        attachPdfHeaders(response, "motabiq-transaction-details-" + from + "-to-" + to + ".pdf");
        reportsService.exportDetailsPdf(tenantId, tenantName(tenantId), from, to, status, response.getOutputStream());
    }

    @GetMapping(value = "/transactions/daily/export.csv", produces = "text/csv; charset=UTF-8")
    public void dailyCsv(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws IOException {
        attachCsvHeaders(response, "motabiq-transactions-daily-" + from + "-to-" + to + ".csv");
        reportsService.exportDailyCsv(tenantId, from, to, status, response.getOutputStream());
    }

    @GetMapping(value = "/transactions/monthly/export.csv", produces = "text/csv; charset=UTF-8")
    public void monthlyCsv(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws IOException {
        attachCsvHeaders(response, "motabiq-transactions-monthly-" + from + "-to-" + to + ".csv");
        reportsService.exportMonthlyCsv(tenantId, from, to, status, response.getOutputStream());
    }

    @GetMapping(value = "/transactions/daily/export.pdf", produces = "application/pdf")
    public void dailyPdf(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws IOException {
        attachPdfHeaders(response, "motabiq-transactions-daily-" + from + "-to-" + to + ".pdf");
        reportsService.exportDailyPdf(tenantId, tenantName(tenantId), from, to, status, response.getOutputStream());
    }

    @GetMapping(value = "/transactions/monthly/export.pdf", produces = "application/pdf")
    public void monthlyPdf(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws IOException {
        attachPdfHeaders(response, "motabiq-transactions-monthly-" + from + "-to-" + to + ".pdf");
        reportsService.exportMonthlyPdf(tenantId, tenantName(tenantId), from, to, status, response.getOutputStream());
    }

    private String tenantName(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(t -> t.getLegalName()).orElse(tenantId.toString());
    }

    private void attachCsvHeaders(HttpServletResponse response, String filename) {
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    }

    private void attachPdfHeaders(HttpServletResponse response, String filename) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    }
}
