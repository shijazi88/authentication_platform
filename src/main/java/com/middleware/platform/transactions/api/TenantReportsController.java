package com.middleware.platform.transactions.api;

import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.iam.security.CurrentTenant;
import com.middleware.platform.transactions.dto.ReportDetailRow;
import com.middleware.platform.transactions.dto.ReportSummary;
import com.middleware.platform.transactions.service.ReportsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The bank's own reports in the client portal — the same summary and
 * transaction-detail reports (and CSV / PDF exports) as the admin Reports page,
 * always scoped to the signed-in bank: there is no {@code tenantId} parameter.
 * Date ranges are inclusive; {@code status} is ALL (default), SUCCESS or FAILED.
 */
@RestController
@RequestMapping("/portal-api/reports")
@RequiredArgsConstructor
public class TenantReportsController {

    private final ReportsService reportsService;
    private final TenantRepository tenantRepository;

    @GetMapping("/transactions/{groupBy:daily|monthly}")
    public ReportSummary summary(
            @PathVariable String groupBy,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status) {
        UUID tenantId = CurrentTenant.id();
        return "monthly".equals(groupBy)
                ? reportsService.monthly(tenantId, from, to, status)
                : reportsService.daily(tenantId, from, to, status);
    }

    @GetMapping("/transactions/details")
    public Page<ReportDetailRow> details(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return reportsService.details(CurrentTenant.id(), from, to, status,
                Math.max(0, page), Math.min(Math.max(1, size), 200));
    }

    @GetMapping(value = "/transactions/{kind:daily|monthly|details}/export.csv", produces = "text/csv; charset=UTF-8")
    public void csv(
            @PathVariable String kind,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws IOException {
        UUID tenantId = CurrentTenant.id();
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName(kind, from, to) + ".csv\"");
        switch (kind) {
            case "monthly" -> reportsService.exportMonthlyCsv(tenantId, from, to, status, response.getOutputStream());
            case "details" -> reportsService.exportDetailsCsv(tenantId, from, to, status, response.getOutputStream());
            default -> reportsService.exportDailyCsv(tenantId, from, to, status, response.getOutputStream());
        }
    }

    @GetMapping(value = "/transactions/{kind:daily|monthly|details}/export.pdf", produces = "application/pdf")
    public void pdf(
            @PathVariable String kind,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "en") String lang,
            HttpServletResponse response) throws IOException {
        UUID tenantId = CurrentTenant.id();
        String tenantName = tenantRepository.findById(tenantId).map(t -> t.getLegalName()).orElse("");
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName(kind, from, to) + ".pdf\"");
        switch (kind) {
            case "monthly" -> reportsService.exportMonthlyPdf(tenantId, tenantName, from, to, status, lang, response.getOutputStream());
            case "details" -> reportsService.exportDetailsPdf(tenantId, tenantName, from, to, status, lang, response.getOutputStream());
            default -> reportsService.exportDailyPdf(tenantId, tenantName, from, to, status, lang, response.getOutputStream());
        }
    }

    private static String fileName(String kind, LocalDate from, LocalDate to) {
        String base = "details".equals(kind) ? "motabiq-transaction-details" : "motabiq-transactions-" + kind;
        return base + "-" + from + "-to-" + to;
    }
}
