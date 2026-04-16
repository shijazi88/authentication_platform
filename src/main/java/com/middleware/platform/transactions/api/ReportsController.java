package com.middleware.platform.transactions.api;

import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.transactions.dto.ReportSummary;
import com.middleware.platform.transactions.service.ReportsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Admin-portal report endpoints.
 *
 * <p>All endpoints accept an optional {@code status} filter:
 * {@code ALL} (default), {@code SUCCESS}, {@code FAILED}.
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

    @GetMapping("/transactions/monthly")
    public ReportSummary monthly(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status) {
        return reportsService.monthly(tenantId, from, to, status);
    }

    @GetMapping(value = "/transactions/daily/export.csv", produces = "text/csv; charset=UTF-8")
    public void dailyCsv(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws IOException {
        attachCsvHeaders(response, "transactions-daily-" + from + "-to-" + to + ".csv");
        reportsService.exportDailyCsv(tenantId, from, to, status, response.getOutputStream());
    }

    @GetMapping(value = "/transactions/monthly/export.csv", produces = "text/csv; charset=UTF-8")
    public void monthlyCsv(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws IOException {
        attachCsvHeaders(response, "transactions-monthly-" + from + "-to-" + to + ".csv");
        reportsService.exportMonthlyCsv(tenantId, from, to, status, response.getOutputStream());
    }

    @GetMapping(value = "/transactions/daily/export.pdf", produces = "application/pdf")
    public void dailyPdf(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws IOException {
        String tenantName = tenantRepository.findById(tenantId)
                .map(t -> t.getLegalName()).orElse(tenantId.toString());
        attachPdfHeaders(response, "transactions-daily-" + from + "-to-" + to + ".pdf");
        reportsService.exportDailyPdf(tenantId, tenantName, from, to, status, response.getOutputStream());
    }

    @GetMapping(value = "/transactions/monthly/export.pdf", produces = "application/pdf")
    public void monthlyPdf(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws IOException {
        String tenantName = tenantRepository.findById(tenantId)
                .map(t -> t.getLegalName()).orElse(tenantId.toString());
        attachPdfHeaders(response, "transactions-monthly-" + from + "-to-" + to + ".pdf");
        reportsService.exportMonthlyPdf(tenantId, tenantName, from, to, status, response.getOutputStream());
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
