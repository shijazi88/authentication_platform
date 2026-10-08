package com.middleware.platform.unit;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.iam.domain.ApiCredential;
import com.middleware.platform.iam.repo.ApiCredentialRepository;
import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.transactions.domain.Transaction;
import com.middleware.platform.transactions.domain.TransactionStatus;
import com.middleware.platform.transactions.dto.ReportSummary;
import com.middleware.platform.transactions.repo.TransactionRepository;
import com.middleware.platform.transactions.service.ReportPdfExporter;
import com.middleware.platform.transactions.service.ReportsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportsServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID CREDENTIAL = UUID.randomUUID();
    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 8);

    private TransactionRepository transactions;
    private ReportsService service;

    @BeforeEach
    void setUp() {
        transactions = mock(TransactionRepository.class);
        ApiCredentialRepository credentials = mock(ApiCredentialRepository.class);
        when(credentials.findAllById(any())).thenReturn(List.of(
                ApiCredential.builder().id(CREDENTIAL).clientId("cli_test").build()));
        service = new ReportsService(transactions, new ReportPdfExporter(), mock(TenantRepository.class), credentials);
    }

    @Test
    @DisplayName("summary includes the whole 'to' day")
    void summaryRangeIsInclusive() {
        stubSummary();
        service.daily(TENANT, FROM, TO, "ALL");
        verify(transactions).dailyReportRaw(TENANT.toString(),
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-09T00:00:00Z"));
    }

    @Test
    @DisplayName("summary breakdown: verdicts, bank-facing failure reasons, performance")
    void summaryBreakdown() {
        stubSummary();
        ReportSummary.Breakdown b = service.daily(TENANT, FROM, TO, "ALL").breakdown();
        assertThat(b.verdicts()).extracting(ReportSummary.VerdictCount::verdict).containsExactly("MATCH", "NO_MATCH");
        assertThat(b.failureReasons()).hasSize(1);
        assertThat(b.failureReasons().get(0).error()).isEqualTo("CONNECTOR_UNAVAILABLE");
        assertThat(b.failureReasons().get(0).message()).doesNotContain("MOI").doesNotContain("breaker")
                .isEqualTo("The verification service is temporarily unavailable. Please try again in a moment.");
        assertThat(b.avgLatencyMs()).isEqualTo(1235L);
        assertThat(b.billableCount()).isEqualTo(9);
    }

    @Test
    @DisplayName("FAILED filter: no verdicts, only failed statuses in the performance figures")
    void failedFilterSkipsVerdicts() {
        stubSummary();
        ReportSummary.Breakdown b = service.daily(TENANT, FROM, TO, "FAILED").breakdown();
        assertThat(b.verdicts()).isEmpty();
        verify(transactions, never()).verdictBreakdownRaw(any(), any(), any());
        verify(transactions).performanceRaw(eq(TENANT.toString()), eq(List.of("FAILED", "TIMEOUT", "REJECTED")), any(), any());
    }

    @Test
    @DisplayName("summary PDF: MOTABIQ branding, plain error names, Arabic client name, one page")
    void summaryPdfBranding() throws Exception {
        stubSummary();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.exportDailyPdf(TENANT, "بنك القطيبي — Al-Qutaibi Bank", FROM, TO, "ALL", "en", out);
        String text = pdfText(out.toByteArray());
        assertThat(text).contains("MOTABIQ — Transaction Report", "Failure reasons", "Verification results",
                "motabiq.ai", "Times in UTC", "2026-10-01 → 2026-10-08", "Al-Qutaibi Bank",
                "Verification service unavailable");
        assertThat(text.toLowerCase()).doesNotContain("sannad").doesNotContain("sanad");
        // Arabic client names need the embedded Arabic-capable font even in English reports
        assertThat(new String(out.toByteArray(), StandardCharsets.ISO_8859_1)).contains("IBMPlexSansArabic");
        assertThat(new PdfReader(out.toByteArray()).getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("Arabic summary and details PDFs embed the Arabic font")
    void arabicPdfs() throws Exception {
        stubSummary();
        ByteArrayOutputStream summary = new ByteArrayOutputStream();
        service.exportDailyPdf(TENANT, "بنك القطيبي", FROM, TO, "ALL", "ar", summary);
        assertThat(new String(summary.toByteArray(), StandardCharsets.ISO_8859_1)).contains("IBMPlexSansArabic-Bold", "IBMPlexSansArabic-Regular");
        assertThat(pdfText(summary.toByteArray())).contains("motabiq.ai");

        stubDetails(List.of(tx(0, TransactionStatus.SUCCESS), tx(1, TransactionStatus.REJECTED)));
        ByteArrayOutputStream details = new ByteArrayOutputStream();
        service.exportDetailsPdf(TENANT, "بنك القطيبي", FROM, TO, "ALL", "ar", details);
        PdfReader reader = new PdfReader(details.toByteArray());
        assertThat(reader.getNumberOfPages()).isEqualTo(1);
        assertThat(new String(details.toByteArray(), StandardCharsets.ISO_8859_1)).contains("IBMPlexSansArabic-Regular");
    }

    @Test
    @DisplayName("details CSV: escaped fields, formula guard, bank-facing message, API key")
    void detailsCsv() {
        Transaction failed = tx(0, TransactionStatus.FAILED);
        failed.setErrorCode(ErrorCode.CONNECTOR_UNAVAILABLE.code());
        failed.setErrorMessage("MOI circuit breaker OPEN (recent upstream failures) — boom");
        Transaction exempt = tx(1, TransactionStatus.SUCCESS);
        exempt.setException(true);
        exempt.setExceptionReason("OTHER");
        exempt.setExceptionNote("=HYPERLINK(\"x\"), note");
        stubDetails(List.of(failed, exempt));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.exportDetailsCsv(TENANT, FROM, TO, "ALL", out);
        String csv = out.toString(StandardCharsets.UTF_8);

        assertThat(csv).startsWith("﻿created_at_utc,transaction_id,");
        assertThat(csv).contains("CONNECTOR_UNAVAILABLE", "temporarily unavailable", "cli_test");
        assertThat(csv).doesNotContain("MOI").doesNotContain("circuit breaker");
        assertThat(csv).contains("\"'=HYPERLINK(\"\"x\"\"), note\"");
        assertThat(csv.lines()).hasSize(3);
    }

    @Test
    @DisplayName("details PDF streams several chunks into one landscape document")
    void detailsPdfAcrossChunks() throws Exception {
        List<Transaction> all = new ArrayList<>();
        IntStream.range(0, 2500).forEach(i -> all.add(tx(i, TransactionStatus.SUCCESS)));
        stubDetails(all);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.exportDetailsPdf(TENANT, "Al-Qutaibi Bank", FROM, TO, "ALL", "en", out);

        PdfReader reader = new PdfReader(out.toByteArray());
        assertThat(reader.getNumberOfPages()).isGreaterThan(10);
        assertThat(reader.getPageSizeWithRotation(1).getWidth()).isGreaterThan(reader.getPageSizeWithRotation(1).getHeight());
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        assertThat(extractor.getTextFromPage(1)).contains("MOTABIQ — Transaction Details", "Time (UTC)");
        String last = extractor.getTextFromPage(reader.getNumberOfPages());
        assertThat(last).contains(all.get(2499).getId().toString(), "2500 transactions");
        // header repeats on continuation pages
        assertThat(extractor.getTextFromPage(2)).contains("Transaction ID");
        verify(transactions, times(3)).reportDetails(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("details PDF with no rows still renders")
    void detailsPdfEmpty() throws Exception {
        stubDetails(List.of());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.exportDetailsPdf(TENANT, "Al-Qutaibi Bank", FROM, TO, "ALL", "en", out);
        assertThat(pdfText(out.toByteArray())).contains("No transactions in this range.");
    }

    // ---------------------------------------------------------------------

    private void stubSummary() {
        List<Object[]> daily = new ArrayList<>();
        daily.add(new Object[]{"2026-10-01", 12L, 10L, 2L, 225000L, "YER"});
        when(transactions.dailyReportRaw(any(), any(), any())).thenReturn(daily);
        List<Object[]> verdicts = new ArrayList<>();
        verdicts.add(new Object[]{"MATCH", 8L});
        verdicts.add(new Object[]{"NO_MATCH", 2L});
        lenient().when(transactions.verdictBreakdownRaw(any(), any(), any())).thenReturn(verdicts);
        List<Object[]> reasons = new ArrayList<>();
        reasons.add(new Object[]{2103, 2L, "MOI circuit breaker OPEN (recent upstream failures)"});
        when(transactions.failureReasonsRaw(any(), any(), any())).thenReturn(reasons);
        List<Object[]> perf = new ArrayList<>();
        perf.add(new Object[]{1234.6, 4100L, 9L, 1L});
        when(transactions.performanceRaw(any(), any(), any(), any())).thenReturn(perf);
    }

    private void stubDetails(List<Transaction> all) {
        when(transactions.reportDetails(eq(TENANT), any(), any(), any(), any())).thenAnswer(inv -> {
            Pageable p = inv.getArgument(4);
            int start = (int) Math.min(p.getOffset(), all.size());
            int end = Math.min(start + p.getPageSize(), all.size());
            return new PageImpl<>(all.subList(start, end), p, all.size());
        });
    }

    private static Transaction tx(int i, TransactionStatus status) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .credentialId(CREDENTIAL)
                .status(status)
                .verdict(status == TransactionStatus.SUCCESS ? "MATCH" : null)
                .billable(status == TransactionStatus.SUCCESS)
                .unitPriceMinor(25000L)
                .currency("YER")
                .latencyMs(1200L + i)
                .deviceId("SN-" + i)
                .imageNfiq2(55)
                .createdAt(Instant.parse("2026-10-01T08:00:00Z").plusSeconds(i))
                .build();
    }

    private static String pdfText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder sb = new StringBuilder();
        for (int p = 1; p <= reader.getNumberOfPages(); p++) sb.append(extractor.getTextFromPage(p)).append('\n');
        return sb.toString();
    }
}
