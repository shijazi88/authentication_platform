package com.middleware.platform.transactions.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders reports as PDF documents — suitable for invoicing attachments,
 * regulatory submissions, and email distribution. Two layouts: the summary
 * ({@link ReportSummary}, A4 portrait) and the per-transaction details
 * ({@link DetailsWriter}, A4 landscape, written in chunks).
 *
 * <p>Both come in English or Arabic ({@code lang} = "en" | "ar", following the
 * portal language). Both embed IBM Plex Sans Arabic (Latin + Arabic, SIL OFL —
 * see resources/fonts/ibm-plex-sans-arabic), so Arabic client names render in
 * English reports too; Arabic reports are laid out right-to-left. All text goes
 * through table cells with an explicit run direction, because OpenPDF only
 * applies bidi ordering and Arabic shaping there. It shapes Arabic into the
 * Unicode presentation forms, so the font must map all of them (Cairo, the
 * portal's web font, does not — letters went missing).
 *
 * <p>Uses OpenPDF (LGPL fork of iText 4.2).
 */
@Component
public class ReportPdfExporter {

    private static final Color BRAND_COLOR = new Color(30, 78, 140);
    private static final Color HEADER_BG = BRAND_COLOR;
    private static final Color STRIPE_BG = new Color(245, 247, 252);
    private static final Color TOTAL_BG = new Color(235, 237, 245);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public void export(ReportSummary report, String tenantName, String statusFilter, String lang, OutputStream out) {
        Style st = Style.of(lang);
        Document doc = new Document(PageSize.A4, 40, 40, 40, 50);
        try {
            PdfWriter.getInstance(doc, out).setPageEvent(new Footer(st));
            doc.open();

            addHeading(doc, st, st.t("title.summary"), tenantName,
                    st.t(report.groupBy()) + "  |  " + st.range(report.from(), report.to()) +
                            "  |  " + st.t("status." + statusKey(statusFilter)) +
                            "  |  " + st.t("generated") + " " + generatedAt());

            // KPI summary
            var totals = report.totals();
            PdfPTable kpiTable = table(st, 4);
            kpiTable.setSpacingAfter(20);
            addKpiCell(kpiTable, st, st.t("kpi.total"), String.valueOf(totals.totalTransactions()));
            addKpiCell(kpiTable, st, st.t("kpi.success"), String.valueOf(totals.successCount()));
            addKpiCell(kpiTable, st, st.t("kpi.failed"), String.valueOf(totals.failedCount()));
            double rate = totals.successRate() * 100;
            addKpiCell(kpiTable, st, st.t("kpi.rate"), new DecimalFormat("#0.0").format(rate) + "%");
            doc.add(kpiTable);

            // Revenue
            if (totals.amountMinor() > 0 && !totals.currency().isEmpty()) {
                addText(doc, st, st.t("revenue") + " " + formatMoney(totals.amountMinor(), totals.currency()),
                        st.revenue, 0, 16);
            }

            // Data table
            PdfPTable table = table(st, 5);
            setWidths(table, st, 2.5f, 1.2f, 1.2f, 1.2f, 2f);
            table.setHeaderRows(1);

            addHeaderCell(table, st, st.t("col.period"));
            addHeaderCell(table, st, st.t("col.total"));
            addHeaderCell(table, st, st.t("col.success"));
            addHeaderCell(table, st, st.t("col.failed"));
            addHeaderCell(table, st, st.t("col.revenue"));

            boolean stripe = false;
            for (ReportRow row : report.rows()) {
                Color bg = stripe ? STRIPE_BG : Color.WHITE;
                addDataCell(table, st, row.period(), st.cell, bg);
                addDataCell(table, st, String.valueOf(row.total()), st.cell, bg);
                addDataCell(table, st, String.valueOf(row.successCount()), st.cell, bg);
                addDataCell(table, st, String.valueOf(row.failedCount()), st.cell, bg);
                addDataCell(table, st, row.amountMinor() > 0
                        ? formatMoney(row.amountMinor(), row.currency()) : "—", st.cell, bg);
                stripe = !stripe;
            }

            // Total row
            addDataCell(table, st, st.t("total"), st.cellBold, TOTAL_BG);
            addDataCell(table, st, String.valueOf(totals.totalTransactions()), st.cellBold, TOTAL_BG);
            addDataCell(table, st, String.valueOf(totals.successCount()), st.cellBold, TOTAL_BG);
            addDataCell(table, st, String.valueOf(totals.failedCount()), st.cellBold, TOTAL_BG);
            addDataCell(table, st, totals.amountMinor() > 0
                    ? formatMoney(totals.amountMinor(), totals.currency()) : "—", st.cellBold, TOTAL_BG);

            doc.add(table);

            addBreakdown(doc, st, report.breakdown(), totals);

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
                                     String lang, OutputStream out) {
        return new DetailsWriter(tenantName, from, to, statusFilter, Style.of(lang), out);
    }

    /** Per-transaction PDF (A4 landscape); rows are flushed to the stream as they are added. */
    public final class DetailsWriter implements AutoCloseable {

        private static final String[] COLUMNS = {
                "d.time", "d.txid", "d.type", "d.status", "d.verdict", "d.code", "d.reason",
                "d.device", "d.nfiq", "d.ms", "d.amount"};

        private final Style st;
        private final Document doc = new Document(PageSize.A4.rotate(), 28, 28, 32, 44);
        private final PdfPTable table;
        private long count;
        private long charged;
        private String currency = "";
        private boolean stripe;

        private DetailsWriter(String tenantName, String from, String to, String statusFilter, Style st,
                              OutputStream out) {
            this.st = st;
            this.table = table(st, COLUMNS.length);
            try {
                PdfWriter.getInstance(doc, out).setPageEvent(new Footer(st));
                doc.open();
                addHeading(doc, st, st.t("title.details"), tenantName,
                        st.range(from, to) + "  |  " + st.t("status." + statusKey(statusFilter)) +
                                "  |  " + st.t("generated") + " " + generatedAt());
                setWidths(table, st, 1.6f, 3.1f, 1.0f, 0.8f, 1.4f, 0.55f, 2.3f, 2.4f, 0.7f, 0.9f, 0.95f);
                table.setHeaderRows(1);
                table.setComplete(false);
                for (String c : COLUMNS) {
                    PdfPCell cell = cell(st, st.t(c), st.smallHeader);
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
                row(TIME.format(r.createdAt()), bg);
                row(r.transactionId().toString(), bg);
                row(st.t("type." + r.type()), bg);
                row(st.status(r.status()), bg);
                row(r.verdict() == null && !"SUCCESS".equals(r.status()) ? "—" : st.verdict(r.verdict()), bg);
                row(r.errorCode() == null ? "" : String.valueOf(r.errorCode()), bg);
                row(reason(st, r), bg);
                row(r.deviceId() == null ? "" : r.deviceId(), bg);
                row(r.imageNfiq2() == null ? "" : String.valueOf(r.imageNfiq2()), bg);
                row(r.latencyMs() == null ? "" : String.valueOf(r.latencyMs()), bg);
                row(r.amountMinor() == null ? "—" : formatMoney(r.amountMinor(), r.currency()), bg);
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
                    addText(doc, st, st.t("none"), st.cell, 0, 0);
                } else {
                    doc.add(table);
                    addText(doc, st, st.t("txCount").replace("{n}", String.valueOf(count))
                                    + (charged > 0 ? "  |  " + st.t("charged") + " " + formatMoney(charged, currency) : ""),
                            st.cellBold, 8, 0);
                }
            } catch (DocumentException e) {
                throw new RuntimeException("Failed to generate PDF report", e);
            } finally {
                doc.close();
            }
        }

        private void row(String text, Color bg) {
            PdfPCell cell = cell(st, text, st.smallCell);
            cell.setBackgroundColor(bg);
            cell.setPadding(3);
            cell.setBorderWidth(0.5f);
            cell.setBorderColor(new Color(230, 230, 230));
            table.addCell(cell);
        }
    }

    // ---------------------------------------------------------------------

    private static void addHeading(Document doc, Style st, String title, String tenantName, String meta)
            throws DocumentException {
        addText(doc, st, title, st.title, 0, 4);
        addText(doc, st, st.t("client") + " " + tenantName, st.subtitle, 0, 0);
        addText(doc, st, meta, st.subtitle, 0, 20);
    }

    /**
     * A line of text, in a borderless one-cell table: OpenPDF only applies bidi
     * ordering and Arabic shaping inside cells and ColumnText.
     */
    private static void addText(Document doc, Style st, String text, Font font, float before, float after)
            throws DocumentException {
        PdfPTable t = table(st, 1);
        t.setSpacingBefore(before);
        t.setSpacingAfter(after);
        PdfPCell c = cell(st, text, font);
        c.setBorder(0);
        c.setPadding(0);
        c.setPaddingBottom(3);
        t.addCell(c);
        doc.add(t);
    }

    private void addBreakdown(Document doc, Style st, ReportSummary.Breakdown b, ReportSummary.Totals totals)
            throws DocumentException {
        if (b == null) return;

        if (!b.verdicts().isEmpty()) {
            addText(doc, st, st.t("sec.results"), st.section, 18, 6);
            PdfPTable t = smallTable(st, new float[]{3f, 1f, 1f}, "col.result", "col.count", "col.share");
            boolean stripe = false;
            for (ReportSummary.VerdictCount v : b.verdicts()) {
                Color bg = stripe ? STRIPE_BG : Color.WHITE;
                addDataCell(t, st, st.verdict(v.verdict()), st.cell, bg);
                addDataCell(t, st, String.valueOf(v.count()), st.cell, bg);
                addDataCell(t, st, share(v.count(), totals.successCount()), st.cell, bg);
                stripe = !stripe;
            }
            doc.add(t);
        }

        if (!b.failureReasons().isEmpty()) {
            addText(doc, st, st.t("sec.failures"), st.section, 18, 6);
            PdfPTable t = smallTable(st, new float[]{0.8f, 5f, 1f, 1f}, "col.code", "col.reason", "col.count", "col.share");
            boolean stripe = false;
            for (ReportSummary.FailureReason f : b.failureReasons()) {
                Color bg = stripe ? STRIPE_BG : Color.WHITE;
                addDataCell(t, st, f.errorCode() == null ? "—" : String.valueOf(f.errorCode()), st.cell, bg);
                addDataCell(t, st, st.failureLabel(f.errorCode(), f.error(), f.message()), st.cell, bg);
                addDataCell(t, st, String.valueOf(f.count()), st.cell, bg);
                addDataCell(t, st, share(f.count(), totals.failedCount()), st.cell, bg);
                stripe = !stripe;
            }
            doc.add(t);
        }

        addText(doc, st, st.t("sec.perf"), st.section, 18, 6);
        PdfPTable kpi = table(st, 4);
        addKpiCell(kpi, st, st.t("perf.avg"), b.avgLatencyMs() == null ? "—" : b.avgLatencyMs() + " ms");
        addKpiCell(kpi, st, st.t("perf.max"), b.maxLatencyMs() == null ? "—" : b.maxLatencyMs() + " ms");
        addKpiCell(kpi, st, st.t("perf.charged"), String.valueOf(b.billableCount()));
        addKpiCell(kpi, st, st.t("perf.exceptions"), String.valueOf(b.exceptionCount()));
        doc.add(kpi);
    }

    private static PdfPTable table(Style st, int columns) {
        PdfPTable t = new PdfPTable(columns);
        t.setWidthPercentage(100);
        t.setRunDirection(st.direction());
        return t;
    }

    /**
     * Column widths in logical (reading) order. OpenPDF lays an RTL table's
     * columns out right-to-left but applies widths left-to-right, so reverse them.
     */
    private static void setWidths(PdfPTable table, Style st, float... widths) throws DocumentException {
        float[] w = widths.clone();
        if (st.rtl) {
            for (int i = 0, j = w.length - 1; i < j; i++, j--) {
                float tmp = w[i]; w[i] = w[j]; w[j] = tmp;
            }
        }
        table.setWidths(w);
    }

    private static PdfPCell cell(Style st, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setRunDirection(st.direction());
        return c;
    }

    private PdfPTable smallTable(Style st, float[] widths, String... headerKeys) throws DocumentException {
        PdfPTable t = table(st, headerKeys.length);
        setWidths(t, st, widths);
        t.setHeaderRows(1);
        for (String h : headerKeys) addHeaderCell(t, st, st.t(h));
        return t;
    }

    private static String share(long part, long whole) {
        return whole == 0 ? "—" : new DecimalFormat("#0.0").format(part * 100.0 / whole) + "%";
    }

    private static String statusKey(String statusFilter) {
        if ("SUCCESS".equalsIgnoreCase(statusFilter)) return "SUCCESS";
        if ("FAILED".equalsIgnoreCase(statusFilter)) return "FAILED";
        return "ALL";
    }

    private static String reason(Style st, ReportDetailRow r) {
        if (r.errorCode() != null || r.error() != null) return st.errorName(r.errorCode(), r.error());
        if (r.exceptionReason() != null) return st.t("exceptionPrefix") + st.exceptionReason(r.exceptionReason());
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

    /** Brand + time-zone note bottom-start and the page number bottom-end of every page. */
    private static final class Footer extends PdfPageEventHelper {
        private final Style st;

        Footer(Style st) { this.st = st; }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float y = document.bottom() - 20;
            int dir = st.direction();
            Phrase brand = new Phrase(st.t("brand") + " · motabiq.ai · " + st.t("utc"), st.footer);
            Phrase page = new Phrase(st.t("page") + " " + writer.getPageNumber(), st.footer);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    st.rtl ? page : brand, document.left(), y, 0, dir, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    st.rtl ? brand : page, document.right(), y, 0, dir, 0);
        }
    }

    private static void addKpiCell(PdfPTable table, Style st, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setRunDirection(st.direction());
        cell.setBorder(0);
        cell.setPadding(8);
        cell.setBackgroundColor(new Color(248, 249, 252));
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", st.kpiLabel));
        p.add(new Chunk(value, st.kpiValue));
        cell.addElement(p);
        table.addCell(cell);
    }

    private static void addHeaderCell(PdfPTable table, Style st, String text) {
        PdfPCell cell = cell(st, text, st.header);
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setBorderWidth(0);
        table.addCell(cell);
    }

    private static void addDataCell(PdfPTable table, Style st, String text, Font font, Color bg) {
        PdfPCell cell = cell(st, text, font);
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new Color(230, 230, 230));
        table.addCell(cell);
    }

    // ---------------------------------------------------------------------

    /** Fonts, direction and wording for one report language. */
    static final class Style {

        final boolean rtl;
        final Map<String, String> labels;
        final Font title, subtitle, section, header, cell, cellBold, smallHeader, smallCell, footer,
                kpiLabel, kpiValue, revenue;

        private Style(boolean rtl, Map<String, String> labels, BaseFont regular, BaseFont bold) {
            this.rtl = rtl;
            this.labels = labels;
            this.title = font(regular, bold, 18, true, BRAND_COLOR);
            this.subtitle = font(regular, bold, 10, false, Color.GRAY);
            this.section = font(regular, bold, 11, true, BRAND_COLOR);
            this.header = font(regular, bold, 9, true, Color.WHITE);
            this.cell = font(regular, bold, 9, false, Color.DARK_GRAY);
            this.cellBold = font(regular, bold, 9, true, Color.DARK_GRAY);
            this.smallHeader = font(regular, bold, 7, true, Color.WHITE);
            this.smallCell = font(regular, bold, 7, false, Color.DARK_GRAY);
            this.footer = font(regular, bold, 7, false, Color.GRAY);
            this.kpiLabel = font(regular, bold, 8, false, Color.GRAY);
            this.kpiValue = font(regular, bold, 14, true, BRAND_COLOR);
            this.revenue = font(regular, bold, 12, true, new Color(212, 160, 23));
        }

        private static Font font(BaseFont regular, BaseFont bold, float size, boolean isBold, Color color) {
            return new Font(isBold ? bold : regular, size, Font.NORMAL, color);
        }

        /** OpenPDF run direction: bidi with an RTL or LTR base, never NO_BIDI (that skips Arabic shaping). */
        int direction() {
            return rtl ? PdfWriter.RUN_DIRECTION_RTL : PdfWriter.RUN_DIRECTION_LTR;
        }

        static Style of(String lang) {
            return "ar".equalsIgnoreCase(lang) ? Holder.AR : Holder.EN;
        }

        /** Lazily built once; both languages embed IBM Plex Sans Arabic. */
        private static final class Holder {
            static final BaseFont REGULAR = loadFont("fonts/ibm-plex-sans-arabic/IBMPlexSansArabic-Regular.ttf");
            static final BaseFont BOLD = loadFont("fonts/ibm-plex-sans-arabic/IBMPlexSansArabic-Bold.ttf");
            static final Style EN = new Style(false, EN_LABELS, REGULAR, BOLD);
            static final Style AR = new Style(true, AR_LABELS, REGULAR, BOLD);
        }

        private static BaseFont loadFont(String resource) {
            try (InputStream in = ReportPdfExporter.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) throw new IllegalStateException("Missing font resource " + resource);
                return BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true,
                        in.readAllBytes(), null);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not load font " + resource, e);
            }
        }

        String t(String key) {
            return labels.getOrDefault(key, key);
        }

        String range(String from, String to) {
            return t("range").replace("{from}", from).replace("{to}", to);
        }

        String verdict(String verdict) {
            if (verdict == null) return t("notRecorded");
            return labels.getOrDefault("verdict." + verdict, humanize(verdict));
        }

        String status(String status) {
            return labels.getOrDefault("txStatus." + status, humanize(status));
        }

        String exceptionReason(String reason) {
            return labels.getOrDefault("exc." + reason, humanize(reason));
        }

        String errorName(Integer code, String error) {
            String named = code == null ? null : labels.get("err." + code);
            return named != null ? named : humanize(error);
        }

        /** Failure-reasons row: the plain error name (same wording as the Support page). */
        String failureLabel(Integer code, String error, String message) {
            String name = errorName(code, error);
            return name.isEmpty() && message != null ? message : name;
        }
    }

    private static final Map<String, String> EN_LABELS = Map.ofEntries(
            Map.entry("brand", "MOTABIQ"),
            Map.entry("title.summary", "MOTABIQ — Transaction Report"),
            Map.entry("title.details", "MOTABIQ — Transaction Details"),
            Map.entry("client", "Client:"),
            Map.entry("daily", "Daily"),
            Map.entry("monthly", "Monthly"),
            Map.entry("range", "{from} → {to}"),
            Map.entry("generated", "Generated:"),
            Map.entry("status.ALL", "All statuses"),
            Map.entry("status.SUCCESS", "Successful only"),
            Map.entry("status.FAILED", "Failed only"),
            Map.entry("kpi.total", "Total Transactions"),
            Map.entry("kpi.success", "Successful"),
            Map.entry("kpi.failed", "Failed"),
            Map.entry("kpi.rate", "Success Rate"),
            Map.entry("revenue", "Total Revenue:"),
            Map.entry("col.period", "Period"),
            Map.entry("col.total", "Total"),
            Map.entry("col.success", "Success"),
            Map.entry("col.failed", "Failed"),
            Map.entry("col.revenue", "Revenue"),
            Map.entry("total", "TOTAL"),
            Map.entry("sec.results", "Verification results (successful transactions)"),
            Map.entry("sec.failures", "Failure reasons"),
            Map.entry("sec.perf", "Performance & billing"),
            Map.entry("col.result", "Result"),
            Map.entry("col.count", "Count"),
            Map.entry("col.share", "Share"),
            Map.entry("col.code", "Code"),
            Map.entry("col.reason", "Reason"),
            Map.entry("perf.avg", "Average response time"),
            Map.entry("perf.max", "Slowest response"),
            Map.entry("perf.charged", "Charged transactions"),
            Map.entry("perf.exceptions", "Fingerprint exceptions"),
            Map.entry("utc", "Times in UTC"),
            Map.entry("d.time", "Time (UTC)"),
            Map.entry("d.txid", "Transaction ID"),
            Map.entry("d.type", "Type"),
            Map.entry("d.status", "Status"),
            Map.entry("d.verdict", "Verdict"),
            Map.entry("d.code", "Code"),
            Map.entry("d.reason", "Reason"),
            Map.entry("d.device", "Device"),
            Map.entry("d.nfiq", "NFIQ 2"),
            Map.entry("d.ms", "Response (ms)"),
            Map.entry("d.amount", "Amount"),
            Map.entry("type.FINGERPRINT", "Fingerprint"),
            Map.entry("type.EXCEPTION", "Exception"),
            Map.entry("none", "No transactions in this range."),
            Map.entry("txCount", "{n} transactions"),
            Map.entry("charged", "Charged:"),
            Map.entry("page", "Page"),
            Map.entry("notRecorded", "Not recorded"),
            Map.entry("exceptionPrefix", "Exception: "),
            Map.entry("verdict.MATCH", "Match"),
            Map.entry("verdict.NO_MATCH", "No match"),
            Map.entry("verdict.NO_VERIFICATION_POSSIBLE", "No biometric on file"),
            Map.entry("verdict.EXEMPT", "Exempt (no fingerprint)"),
            // Plain error names — the Support page's playbook wording (portal-admin/src/data/errorPlaybook.ts)
            Map.entry("err.1001", "Bad request"),
            Map.entry("err.1002", "Validation failed / invalid biometrics"),
            Map.entry("err.1003", "Fingerprint image quality rejected"),
            Map.entry("err.1004", "Fingerprint image format rejected"),
            Map.entry("err.1101", "Authentication required"),
            Map.entry("err.1102", "Invalid credentials"),
            Map.entry("err.1201", "Access denied (IP allow-list)"),
            Map.entry("err.1202", "Not entitled by plan"),
            Map.entry("err.1203", "PIN unlock required"),
            Map.entry("err.1204", "Invalid PIN"),
            Map.entry("err.1205", "Capture device not registered"),
            Map.entry("err.1301", "National number not found"),
            Map.entry("err.1302", "Fingerprint did not match the ID"),
            Map.entry("err.1401", "Conflict"),
            Map.entry("err.1402", "Quota / rate limit exceeded"),
            Map.entry("err.1403", "Insufficient wallet balance"),
            Map.entry("err.2001", "Internal server error"),
            Map.entry("err.2101", "Verification service error"),
            Map.entry("err.2102", "Verification service timed out"),
            Map.entry("err.2103", "Verification service unavailable"));

    /** Arabic wording — status, verdict, exception and error names match the portal's ar.json / support playbook. */
    private static final Map<String, String> AR_LABELS = Map.ofEntries(
            Map.entry("brand", "مطابق"),
            Map.entry("title.summary", "مطابق — تقرير المعاملات"),
            Map.entry("title.details", "مطابق — تفاصيل المعاملات"),
            Map.entry("client", "العميل:"),
            Map.entry("daily", "يومي"),
            Map.entry("monthly", "شهري"),
            Map.entry("range", "من {from} إلى {to}"),
            Map.entry("generated", "تاريخ الإنشاء:"),
            Map.entry("status.ALL", "جميع الحالات"),
            Map.entry("status.SUCCESS", "الناجحة فقط"),
            Map.entry("status.FAILED", "الفاشلة فقط"),
            Map.entry("kpi.total", "إجمالي المعاملات"),
            Map.entry("kpi.success", "الناجحة"),
            Map.entry("kpi.failed", "الفاشلة"),
            Map.entry("kpi.rate", "نسبة النجاح"),
            Map.entry("revenue", "إجمالي الإيراد:"),
            Map.entry("col.period", "الفترة"),
            Map.entry("col.total", "الإجمالي"),
            Map.entry("col.success", "ناجحة"),
            Map.entry("col.failed", "فاشلة"),
            Map.entry("col.revenue", "الإيراد"),
            Map.entry("total", "الإجمالي"),
            Map.entry("sec.results", "نتائج التحقق (المعاملات الناجحة)"),
            Map.entry("sec.failures", "أسباب الفشل"),
            Map.entry("sec.perf", "الأداء والفوترة"),
            Map.entry("col.result", "النتيجة"),
            Map.entry("col.count", "العدد"),
            Map.entry("col.share", "النسبة"),
            Map.entry("col.code", "الرمز"),
            Map.entry("col.reason", "السبب"),
            Map.entry("perf.avg", "متوسط زمن الاستجابة"),
            Map.entry("perf.max", "أبطأ استجابة"),
            Map.entry("perf.charged", "المعاملات المحتسبة"),
            Map.entry("perf.exceptions", "استثناءات البصمة"),
            Map.entry("utc", "الأوقات بتوقيت UTC"),
            Map.entry("d.time", "الوقت (UTC)"),
            Map.entry("d.txid", "رقم المعاملة"),
            Map.entry("d.type", "النوع"),
            Map.entry("d.status", "الحالة"),
            Map.entry("d.verdict", "النتيجة"),
            Map.entry("d.code", "الرمز"),
            Map.entry("d.reason", "السبب"),
            Map.entry("d.device", "الجهاز"),
            Map.entry("d.nfiq", "NFIQ 2"),
            Map.entry("d.ms", "الاستجابة (ms)"),
            Map.entry("d.amount", "المبلغ"),
            Map.entry("type.FINGERPRINT", "بصمة"),
            Map.entry("type.EXCEPTION", "استثناء"),
            Map.entry("none", "لا توجد معاملات في هذا النطاق."),
            Map.entry("txCount", "{n} معاملة"),
            Map.entry("charged", "المبلغ المحتسب:"),
            Map.entry("page", "صفحة"),
            Map.entry("notRecorded", "غير مسجّلة"),
            Map.entry("exceptionPrefix", "استثناء: "),
            Map.entry("verdict.MATCH", "مطابقة"),
            Map.entry("verdict.NO_MATCH", "عدم مطابقة"),
            Map.entry("verdict.NO_VERIFICATION_POSSIBLE", "لا توجد بصمة مسجلة"),
            Map.entry("verdict.EXEMPT", "معفى (بدون بصمة)"),
            Map.entry("txStatus.SUCCESS", "ناجح"),
            Map.entry("txStatus.FAILED", "فاشل"),
            Map.entry("txStatus.TIMEOUT", "انتهت المهلة"),
            Map.entry("txStatus.REJECTED", "مرفوض"),
            Map.entry("txStatus.INITIATED", "مُبتدأ"),
            Map.entry("exc.HAND_INJURY", "إصابة في اليد"),
            Map.entry("exc.AMPUTATION", "بتر"),
            Map.entry("exc.WORN_PRINTS", "بصمات متآكلة"),
            Map.entry("exc.MEDICAL", "حالة طبية"),
            Map.entry("exc.OTHER", "أخرى"),
            Map.entry("err.1001", "طلب غير صحيح"),
            Map.entry("err.1002", "فشل التحقّق / بصمة غير صالحة"),
            Map.entry("err.1003", "رُفضت جودة صورة البصمة"),
            Map.entry("err.1004", "رُفضت صيغة صورة البصمة"),
            Map.entry("err.1101", "المصادقة مطلوبة"),
            Map.entry("err.1102", "بيانات اعتماد غير صحيحة"),
            Map.entry("err.1201", "الوصول مرفوض (قائمة IP)"),
            Map.entry("err.1202", "غير مشمول بالباقة"),
            Map.entry("err.1203", "يلزم إدخال رمز PIN"),
            Map.entry("err.1204", "رمز PIN غير صحيح"),
            Map.entry("err.1205", "جهاز الالتقاط غير مسجّل لهذا العميل"),
            Map.entry("err.1301", "الرقم الوطني غير موجود"),
            Map.entry("err.1302", "البصمة لا تطابق الرقم الوطني"),
            Map.entry("err.1401", "تعارض"),
            Map.entry("err.1402", "تجاوز الحدّ/الحصة"),
            Map.entry("err.1403", "رصيد المحفظة غير كافٍ"),
            Map.entry("err.2001", "خطأ داخلي في الخادم"),
            Map.entry("err.2101", "خطأ في خدمة التحقّق"),
            Map.entry("err.2102", "انتهت مهلة خدمة التحقّق"),
            Map.entry("err.2103", "خدمة التحقّق غير متاحة"));
}
