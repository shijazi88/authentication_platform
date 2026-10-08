package com.middleware.platform.transactions.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.middleware.platform.transactions.dto.ReportDetailRow;
import com.middleware.platform.transactions.dto.ReportRow;
import com.middleware.platform.transactions.dto.ReportSummary;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Renders reports as PDF documents — suitable for invoicing attachments,
 * regulatory submissions, and email distribution. Two layouts: the summary
 * ({@link ReportSummary}, A4 portrait) and the per-transaction details
 * ({@link DetailsWriter}, A4 landscape, written in chunks).
 *
 * <p>Uses OpenPDF (LGPL fork of iText 4.2). No external fonts required.
 */
@Component
public class ReportPdfExporter {

    static final String BRAND = "MOTABIQ";

    private static final Color BRAND_COLOR = new Color(30, 78, 140);
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, BRAND_COLOR);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, BRAND_COLOR);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
    private static final Font SMALL_HEADER_FONT = new Font(Font.HELVETICA, 7, Font.BOLD, Color.WHITE);
    private static final Font SMALL_CELL_FONT = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY);
    private static final Font KPI_LABEL = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Font KPI_VALUE = new Font(Font.HELVETICA, 14, Font.BOLD, BRAND_COLOR);
    private static final Color HEADER_BG = BRAND_COLOR;
    private static final Color STRIPE_BG = new Color(245, 247, 252);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public void export(ReportSummary report, String tenantName, String statusFilter, OutputStream out) {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 50);
        try {
            PdfWriter.getInstance(doc, out).setPageEvent(new Footer());
            doc.open();

            addHeading(doc, BRAND + " — Transaction Report",
                    "Client: " + tenantName + "  |  " +
                            report.groupBy().substring(0, 1).toUpperCase() + report.groupBy().substring(1) +
                            "  |  " + report.from() + " → " + report.to() +
                            "  |  " + statusLabel(statusFilter) +
                            "  |  Generated: " + generatedAt());

            // KPI summary
            var totals = report.totals();
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(20);
            addKpiCell(kpiTable, "Total Transactions", String.valueOf(totals.totalTransactions()));
            addKpiCell(kpiTable, "Successful", String.valueOf(totals.successCount()));
            addKpiCell(kpiTable, "Failed", String.valueOf(totals.failedCount()));
            double rate = totals.successRate() * 100;
            addKpiCell(kpiTable, "Success Rate", new DecimalFormat("#0.0").format(rate) + "%");
            doc.add(kpiTable);

            // Revenue
            if (totals.amountMinor() > 0 && !totals.currency().isEmpty()) {
                Paragraph revenue = new Paragraph(
                        "Total Revenue: " + formatMoney(totals.amountMinor(), totals.currency()),
                        new Font(Font.HELVETICA, 12, Font.BOLD, new Color(212, 160, 23)));
                revenue.setSpacingAfter(16);
                doc.add(revenue);
            }

            // Data table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.5f, 1.2f, 1.2f, 1.2f, 2f});
            table.setHeaderRows(1);

            addHeaderCell(table, "Period");
            addHeaderCell(table, "Total");
            addHeaderCell(table, "Success");
            addHeaderCell(table, "Failed");
            addHeaderCell(table, "Revenue");

            boolean stripe = false;
            for (ReportRow row : report.rows()) {
                Color bg = stripe ? STRIPE_BG : Color.WHITE;
                addDataCell(table, row.period(), bg);
                addDataCell(table, String.valueOf(row.total()), bg);
                addDataCell(table, String.valueOf(row.successCount()), bg);
                addDataCell(table, String.valueOf(row.failedCount()), bg);
                addDataCell(table, row.amountMinor() > 0
                        ? formatMoney(row.amountMinor(), row.currency()) : "—", bg);
                stripe = !stripe;
            }

            // Total row
            Color totalBg = new Color(235, 237, 245);
            addDataCellBold(table, "TOTAL", totalBg);
            addDataCellBold(table, String.valueOf(totals.totalTransactions()), totalBg);
            addDataCellBold(table, String.valueOf(totals.successCount()), totalBg);
            addDataCellBold(table, String.valueOf(totals.failedCount()), totalBg);
            addDataCellBold(table, totals.amountMinor() > 0
                    ? formatMoney(totals.amountMinor(), totals.currency()) : "—", totalBg);

            doc.add(table);

            addBreakdown(doc, report.breakdown(), totals);

            // Footer
            Paragraph footer = new Paragraph(
                    "\nThis report was generated automatically by the " + BRAND + " platform (motabiq.ai). "
                    + "Times are UTC. Use the transaction details report for a line-by-line listing.",
                    new Font(Font.HELVETICA, 7, Font.ITALIC, Color.GRAY));
            footer.setSpacingBefore(24);
            doc.add(footer);

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        } finally {
            doc.close();
        }
    }

    /**
     * Starts a per-transaction report. Feed rows with {@link DetailsWriter#addRows}
     * (in as many chunks as needed) and close it to finish the document.
     */
    public DetailsWriter openDetails(String tenantName, String from, String to, String statusFilter,
                                     OutputStream out) {
        return new DetailsWriter(tenantName, from, to, statusFilter, out);
    }

    /** Per-transaction PDF (A4 landscape); rows are flushed to the stream as they are added. */
    public final class DetailsWriter implements AutoCloseable {

        private static final String[] COLUMNS = {
                "Time (UTC)", "Transaction ID", "Type", "Status", "Verdict", "Code", "Reason",
                "Device", "NFIQ 2", "ms", "Amount"};

        private final Document doc = new Document(PageSize.A4.rotate(), 28, 28, 32, 44);
        private final PdfPTable table = new PdfPTable(COLUMNS.length);
        private long count;
        private long charged;
        private String currency = "";
        private boolean stripe;

        private DetailsWriter(String tenantName, String from, String to, String statusFilter, OutputStream out) {
            try {
                PdfWriter.getInstance(doc, out).setPageEvent(new Footer());
                doc.open();
                addHeading(doc, BRAND + " — Transaction Details",
                        "Client: " + tenantName + "  |  " + from + " → " + to +
                                "  |  " + statusLabel(statusFilter) + "  |  Generated: " + generatedAt());
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1.7f, 3.1f, 1.1f, 1.0f, 1.5f, 0.6f, 2.6f, 1.6f, 0.7f, 0.7f, 1.1f});
                table.setHeaderRows(1);
                table.setComplete(false);
                for (String c : COLUMNS) {
                    PdfPCell cell = new PdfPCell(new Phrase(c, SMALL_HEADER_FONT));
                    cell.setBackgroundColor(HEADER_BG);
                    cell.setPadding(4);
                    cell.setBorderWidth(0);
                    table.addCell(cell);
                }
            } catch (DocumentException e) {
                doc.close();
                throw new RuntimeException("Failed to generate PDF report", e);
            }
        }

        public void addRows(List<ReportDetailRow> rows) {
            if (rows.isEmpty()) return;
            for (ReportDetailRow r : rows) {
                Color bg = stripe ? STRIPE_BG : Color.WHITE;
                cell(TIME.format(r.createdAt()), bg);
                cell(r.transactionId().toString(), bg);
                cell("EXCEPTION".equals(r.type()) ? "Exception" : "Fingerprint", bg);
                cell(humanize(r.status()), bg);
                cell(r.verdict() == null && !"SUCCESS".equals(r.status()) ? "—" : verdictLabel(r.verdict()), bg);
                cell(r.errorCode() == null ? "" : String.valueOf(r.errorCode()), bg);
                cell(reason(r), bg);
                cell(r.deviceId() == null ? "" : r.deviceId(), bg);
                cell(r.imageNfiq2() == null ? "" : String.valueOf(r.imageNfiq2()), bg);
                cell(r.latencyMs() == null ? "" : String.valueOf(r.latencyMs()), bg);
                cell(r.amountMinor() == null ? "—" : formatMoney(r.amountMinor(), r.currency()), bg);
                stripe = !stripe;
                count++;
                if (r.amountMinor() != null) {
                    charged += r.amountMinor();
                    if (currency.isEmpty() && r.currency() != null) currency = r.currency();
                }
            }
            try {
                doc.add(table); // flushes the completed rows; the header repeats on each page
            } catch (DocumentException e) {
                throw new RuntimeException("Failed to generate PDF report", e);
            }
        }

        @Override
        public void close() {
            try {
                table.setComplete(true);
                if (count == 0) {
                    doc.add(new Paragraph("No transactions in this range.", CELL_FONT));
                } else {
                    doc.add(table);
                    Paragraph total = new Paragraph(count + " transactions"
                            + (charged > 0 ? "  |  Charged: " + formatMoney(charged, currency) : ""),
                            new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY));
                    total.setSpacingBefore(8);
                    doc.add(total);
                }
            } catch (DocumentException e) {
                throw new RuntimeException("Failed to generate PDF report", e);
            } finally {
                doc.close();
            }
        }

        private void cell(String text, Color bg) {
            PdfPCell cell = new PdfPCell(new Phrase(text, SMALL_CELL_FONT));
            cell.setBackgroundColor(bg);
            cell.setPadding(3);
            cell.setBorderWidth(0.5f);
            cell.setBorderColor(new Color(230, 230, 230));
            table.addCell(cell);
        }
    }

    // ---------------------------------------------------------------------

    private void addHeading(Document doc, String title, String subtitle) throws DocumentException {
        Paragraph t = new Paragraph(title, TITLE_FONT);
        t.setSpacingAfter(4);
        doc.add(t);
        Paragraph s = new Paragraph(subtitle, SUBTITLE_FONT);
        s.setSpacingAfter(20);
        doc.add(s);
    }

    private void addBreakdown(Document doc, ReportSummary.Breakdown b, ReportSummary.Totals totals)
            throws DocumentException {
        if (b == null) return;

        if (!b.verdicts().isEmpty()) {
            addSection(doc, "Verification results (successful transactions)");
            PdfPTable t = smallTable(new float[]{3f, 1f, 1f}, "Result", "Count", "Share");
            boolean stripe = false;
            for (ReportSummary.VerdictCount v : b.verdicts()) {
                Color bg = stripe ? STRIPE_BG : Color.WHITE;
                addDataCell(t, verdictLabel(v.verdict()), bg);
                addDataCell(t, String.valueOf(v.count()), bg);
                addDataCell(t, share(v.count(), totals.successCount()), bg);
                stripe = !stripe;
            }
            doc.add(t);
        }

        if (!b.failureReasons().isEmpty()) {
            addSection(doc, "Failure reasons");
            PdfPTable t = smallTable(new float[]{0.8f, 5f, 1f, 1f}, "Code", "Reason", "Count", "Share");
            boolean stripe = false;
            for (ReportSummary.FailureReason f : b.failureReasons()) {
                Color bg = stripe ? STRIPE_BG : Color.WHITE;
                addDataCell(t, f.errorCode() == null ? "—" : String.valueOf(f.errorCode()), bg);
                String label = f.error() == null ? "" : humanize(f.error());
                addDataCell(t, f.message() == null ? label : label + " — " + f.message(), bg);
                addDataCell(t, String.valueOf(f.count()), bg);
                addDataCell(t, share(f.count(), totals.failedCount()), bg);
                stripe = !stripe;
            }
            doc.add(t);
        }

        addSection(doc, "Performance & billing");
        PdfPTable kpi = new PdfPTable(4);
        kpi.setWidthPercentage(100);
        addKpiCell(kpi, "Average response time", b.avgLatencyMs() == null ? "—" : b.avgLatencyMs() + " ms");
        addKpiCell(kpi, "Slowest response", b.maxLatencyMs() == null ? "—" : b.maxLatencyMs() + " ms");
        addKpiCell(kpi, "Charged transactions", String.valueOf(b.billableCount()));
        addKpiCell(kpi, "Fingerprint exceptions", String.valueOf(b.exceptionCount()));
        doc.add(kpi);
    }

    private void addSection(Document doc, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, SECTION_FONT);
        p.setSpacingBefore(18);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private PdfPTable smallTable(float[] widths, String... headers) throws DocumentException {
        PdfPTable t = new PdfPTable(headers.length);
        t.setWidthPercentage(100);
        t.setWidths(widths);
        t.setHeaderRows(1);
        for (String h : headers) addHeaderCell(t, h);
        return t;
    }

    private static String share(long part, long whole) {
        return whole == 0 ? "—" : new DecimalFormat("#0.0").format(part * 100.0 / whole) + "%";
    }

    private static String statusLabel(String statusFilter) {
        if ("SUCCESS".equalsIgnoreCase(statusFilter)) return "Successful only";
        if ("FAILED".equalsIgnoreCase(statusFilter)) return "Failed only";
        return "All statuses";
    }

    static String verdictLabel(String verdict) {
        if (verdict == null) return "Not recorded";
        return switch (verdict) {
            case "MATCH" -> "Match";
            case "NO_MATCH" -> "No match";
            case "NO_VERIFICATION_POSSIBLE" -> "No biometric on file";
            case "EXEMPT" -> "Exempt (no fingerprint)";
            default -> humanize(verdict);
        };
    }

    private static String reason(ReportDetailRow r) {
        if (r.error() != null) return humanize(r.error());
        if (r.exceptionReason() != null) return "Exception: " + humanize(r.exceptionReason());
        return "";
    }

    /** IMAGE_QUALITY_REJECTED → "Image quality rejected". */
    static String humanize(String enumName) {
        if (enumName == null || enumName.isEmpty()) return "";
        String s = enumName.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String generatedAt() {
        return Instant.now().toString().substring(0, 16).replace('T', ' ') + " UTC";
    }

    private static String formatMoney(long minor, String currency) {
        if ("YER".equals(currency)) {
            return minor / 100 + " " + currency;
        }
        return new DecimalFormat("#,##0.00").format(minor / 100.0) + " " + currency;
    }

    /** "MOTABIQ · motabiq.ai" bottom-left and the page number bottom-right of every page. */
    private static final class Footer extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float y = document.bottom() - 20;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase(BRAND + " · motabiq.ai", FOOTER_FONT), document.left(), y, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), FOOTER_FONT), document.right(), y, 0);
        }
    }

    private void addKpiCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        cell.setPadding(8);
        cell.setBackgroundColor(new Color(248, 249, 252));
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", KPI_LABEL));
        p.add(new Chunk(value, KPI_VALUE));
        cell.addElement(p);
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setBorderWidth(0);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL_FONT));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new Color(230, 230, 230));
        table.addCell(cell);
    }

    private void addDataCellBold(PdfPTable table, String text, Color bg) {
        Font bold = new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY);
        PdfPCell cell = new PdfPCell(new Phrase(text, bold));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new Color(230, 230, 230));
        table.addCell(cell);
    }
}
