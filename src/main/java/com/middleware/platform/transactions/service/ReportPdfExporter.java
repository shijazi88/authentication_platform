package com.middleware.platform.transactions.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.middleware.platform.transactions.dto.ReportDetailRow;
import com.middleware.platform.transactions.dto.ReportRow;
import com.middleware.platform.transactions.dto.ReportSummary;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import static com.middleware.platform.transactions.service.ReportPdfKit.*;
import static com.middleware.platform.transactions.service.ReportPdfStyle.*;

/**
 * Renders the report PDFs that banks and management receive, in English or
 * Arabic ({@code lang} = "en" | "ar", following the portal language):
 * <ul>
 *   <li><b>Verification Activity Report</b> (A4 portrait) — cover band,
 *       executive summary with KPIs and a written summary, activity chart and
 *       table, verification results, failure analysis, performance and
 *       billing, definitions;</li>
 *   <li><b>Transaction Detail Report</b> (A4 landscape) — one row per
 *       transaction, written in chunks via {@link DetailsWriter}.</li>
 * </ul>
 * Both carry a running header and a "Page i of n" footer, added in a second
 * pass once the page count is known. Building blocks live in
 * {@link ReportPdfKit}; wording and formatting in {@link ReportPdfStyle}.
 *
 * <p>Uses OpenPDF (LGPL fork of iText 4.2).
 */
@Component
public class ReportPdfExporter {

    private static final float MARGIN = 40;
    private static final float MARGIN_WIDE = 28;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public void export(ReportSummary report, String tenantName, String statusFilter, String lang, OutputStream out) {
        ReportPdfStyle st = ReportPdfStyle.of(lang);
        LocalDate from = LocalDate.parse(report.from());
        LocalDate to = LocalDate.parse(report.to());
        String reportName = st.t("report." + report.groupBy());

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, 58, 58);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, buf);
            doc.open();
            float band = 104;
            coverBand(writer, doc, st, band, st.t("title.summary"), tenantName,
                    st.range(from, to) + "   ·   " + reportName);
            spacer(doc, band + 16 - doc.topMargin());
            doc.add(infoGrid(st,
                    new String[]{st.t("info.client"), tenantName},
                    new String[]{st.t("info.report"), reportName},
                    new String[]{st.t("info.period"), st.range(from, to)},
                    new String[]{st.t("info.statuses"), st.t("status." + statusKey(statusFilter))},
                    new String[]{st.t("info.generated"), st.generatedAt()},
                    new String[]{st.t("info.preparedBy"), st.t("preparedBy")}));

            int n = 1;
            n = executiveSummary(doc, st, n, report, tenantName, from, to);
            n = activity(doc, st, n, report);
            n = results(doc, st, n, report);
            n = failures(doc, st, n, report);
            n = performance(doc, st, n, report);
            definitions(doc, st, n);
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
        decorate(buf.toByteArray(), out, st, MARGIN, st.t("title.summary") + "  ·  " + tenantName, tenantName);
    }

    // ---------------------------------------------------------------- summary

    private int executiveSummary(Document doc, ReportPdfStyle st, int n, ReportSummary report, String tenant,
                                 LocalDate from, LocalDate to) throws DocumentException {
        var totals = report.totals();
        var b = report.breakdown();
        long match = verdictCount(b, "MATCH");
        long noMatch = verdictCount(b, "NO_MATCH");
        ReportRow busiest = report.rows().stream().max(Comparator.comparingLong(ReportRow::total)).orElse(null);
        boolean daily = "daily".equals(report.groupBy());

        PdfPTable row1 = table(st, 1, 0.05f, 1, 0.05f, 1);
        row1.addCell(kpi(st, st.t("kpi.requests"), number(totals.totalTransactions()),
                st.fmt(daily ? "kpi.requests.days" : "kpi.requests.months", "n", number(report.rows().size())), NAVY));
        row1.addCell(gutter());
        row1.addCell(kpi(st, st.t("kpi.success"), number(totals.successCount()),
                st.fmt("kpi.success.cap", "rate", share(totals.successCount(), totals.totalTransactions())), GREEN));
        row1.addCell(gutter());
        row1.addCell(kpi(st, st.t("kpi.amount"), money(totals.amountMinor(), totals.currency()),
                st.fmt("kpi.amount.cap", "n", number(b == null ? 0 : b.billableCount())), AZURE));

        PdfPTable row2 = table(st, 1, 0.05f, 1, 0.05f, 1);
        row2.addCell(kpi(st, st.t("kpi.match"), share(match, match + noMatch),
                st.fmt("kpi.match.cap", "match", number(match), "noMatch", number(noMatch)), GREEN));
        row2.addCell(gutter());
        row2.addCell(kpi(st, st.t("kpi.latency"),
                b == null || b.avgLatencyMs() == null ? "—" : number(b.avgLatencyMs()) + " ms",
                b == null || b.maxLatencyMs() == null ? "" : st.fmt("kpi.latency.cap", "max", number(b.maxLatencyMs()) + " ms"),
                SKY));
        row2.addCell(gutter());
        row2.addCell(kpi(st, st.t(daily ? "kpi.busiest.daily" : "kpi.busiest.monthly"),
                busiest == null ? "—" : st.period(busiest.period()),
                busiest == null ? "" : st.fmt("kpi.busiest.cap", "n", number(busiest.total())), NAVY));

        PdfPCell narrative = cell(st, narrative(st, report, tenant, from, to, match, noMatch), st.font(10, INK));
        narrative.setLeading(0, 1.5f);
        narrative.setBackgroundColor(SOFT);
        narrative.setBorder(st.rtl ? Rectangle.RIGHT : Rectangle.LEFT);
        narrative.setBorderColor(AZURE);
        narrative.setBorderWidth(3);
        narrative.setPadding(12);
        narrative.setPaddingBottom(14);

        section(doc, st, n, st.t("sec.summary"), true, wrap(st, row1), gap(9), wrap(st, row2), gap(12), narrative);
        return n + 1;
    }

    private String narrative(ReportPdfStyle st, ReportSummary report, String tenant, LocalDate from, LocalDate to,
                             long match, long noMatch) {
        var totals = report.totals();
        if (totals.totalTransactions() == 0) return st.t("narr.none");
        StringBuilder s = new StringBuilder(st.fmt("narr.main",
                "from", st.date(from), "to", st.date(to), "client", tenant,
                "total", number(totals.totalTransactions()), "success", number(totals.successCount()),
                "rate", share(totals.successCount(), totals.totalTransactions()),
                "amount", money(totals.amountMinor(), totals.currency())));
        var b = report.breakdown();
        if (totals.failedCount() > 0 && b != null && !b.failureReasons().isEmpty()) {
            var top = b.failureReasons().get(0);
            s.append(st.fmt("narr.failed", "failed", number(totals.failedCount()),
                    "reason", st.errorName(top.errorCode(), top.error()), "count", number(top.count())));
        }
        if (match + noMatch > 0) {
            s.append(st.fmt("narr.verdicts", "match", number(match), "noMatch", number(noMatch)));
        }
        return s.toString();
    }

    private int activity(Document doc, ReportPdfStyle st, int n, ReportSummary report) throws DocumentException {
        List<ReportRow> rows = report.rows();
        if (rows.isEmpty()) {
            section(doc, st, n, st.t("sec.activity"), true, note(st, st.t("none")));
            return n + 1;
        }
        section(doc, st, n, st.t("sec.activity"), true, trendCell(st, rows, 180));

        PdfPTable t = table(st, 2.2f, 1.1f, 1.1f, 1.1f, 1.2f, 1.6f);
        t.setSpacingBefore(12);
        t.setHeaderRows(1);
        t.setKeepTogether(rows.size() <= 15); // short tables never split off a lone total row
        headerRow(t, st, st.bold(8.5f, Color.WHITE), false,
                "col.period", "col.requests", "col.success", "col.failed", "col.rate", "col.amount");
        Font body = st.font(9, INK);
        boolean stripe = false;
        for (ReportRow r : rows) {
            Color bg = stripe ? SOFT : Color.WHITE;
            add(t, bg, cell(st, st.period(r.period()), body),
                    numberCell(st, number(r.total()), body),
                    numberCell(st, number(r.successCount()), st.font(9, GREEN)),
                    numberCell(st, number(r.failedCount()), st.font(9, r.failedCount() > 0 ? RED : MUTED)),
                    numberCell(st, share(r.successCount(), r.total()), body),
                    numberCell(st, r.amountMinor() > 0 ? money(r.amountMinor(), r.currency()) : "—", body));
            stripe = !stripe;
        }
        var totals = report.totals();
        Font bold = st.bold(9, NAVY);
        PdfPCell[] totalRow = {cell(st, st.t("total"), bold),
                numberCell(st, number(totals.totalTransactions()), bold),
                numberCell(st, number(totals.successCount()), bold),
                numberCell(st, number(totals.failedCount()), bold),
                numberCell(st, share(totals.successCount(), totals.totalTransactions()), bold),
                numberCell(st, totals.amountMinor() > 0 ? money(totals.amountMinor(), totals.currency()) : "—", bold)};
        for (PdfPCell c : totalRow) {
            c.setBorder(Rectangle.TOP);
            c.setBorderColor(NAVY);
            c.setBorderWidth(1);
        }
        add(t, TRACK, totalRow);
        doc.add(t);
        return n + 1;
    }

    private int results(Document doc, ReportPdfStyle st, int n, ReportSummary report) throws DocumentException {
        var b = report.breakdown();
        if (b == null || b.verdicts().isEmpty()) return n;
        long success = report.totals().successCount();
        PdfPTable t = table(st, 2.3f, 3.2f, 0.9f, 0.9f);
        headerRow(t, st, st.bold(8.5f, MUTED), true, "col.result", "", "col.count", "col.share");
        Font body = st.font(9.5f, INK);
        for (var v : b.verdicts()) {
            add(t, Color.WHITE, cell(st, st.verdict(v.verdict()), body),
                    barCell(st, success == 0 ? 0 : (double) v.count() / success, verdictColor(v.verdict())),
                    numberCell(st, number(v.count()), body),
                    numberCell(st, share(v.count(), success), body));
        }
        section(doc, st, n, st.t("sec.results"), true, wrap(st, t), note(st, st.t("note.results")));
        return n + 1;
    }

    private int failures(Document doc, ReportPdfStyle st, int n, ReportSummary report) throws DocumentException {
        var b = report.breakdown();
        if (b == null || b.failureReasons().isEmpty()) return n;
        long failed = report.totals().failedCount();
        PdfPTable t = table(st, 0.7f, 2.7f, 2.1f, 0.9f, 0.9f);
        headerRow(t, st, st.bold(8.5f, MUTED), true, "col.code", "col.reason", "", "col.count", "col.share");
        Font body = st.font(9.5f, INK);
        for (var f : b.failureReasons()) {
            add(t, Color.WHITE, cell(st, f.errorCode() == null ? "—" : String.valueOf(f.errorCode()), st.font(9.5f, MUTED)),
                    cell(st, st.errorName(f.errorCode(), f.error()), body),
                    barCell(st, failed == 0 ? 0 : (double) f.count() / failed, RED),
                    numberCell(st, number(f.count()), body),
                    numberCell(st, share(f.count(), failed), body));
        }
        section(doc, st, n, st.t("sec.failures"), true, wrap(st, t), note(st, st.t("note.failures")));
        return n + 1;
    }

    private int performance(Document doc, ReportPdfStyle st, int n, ReportSummary report) throws DocumentException {
        var b = report.breakdown();
        if (b == null) return n;
        var totals = report.totals();
        long notCharged = totals.totalTransactions() - b.billableCount();
        String price = b.billableCount() == 0 ? "—" : money(totals.amountMinor() / b.billableCount(), totals.currency());
        PdfPTable grid = infoGrid(st,
                new String[]{st.t("perf.avg"), b.avgLatencyMs() == null ? "—" : number(b.avgLatencyMs()) + " ms"},
                new String[]{st.t("perf.max"), b.maxLatencyMs() == null ? "—" : number(b.maxLatencyMs()) + " ms"},
                new String[]{st.t("perf.charged"), number(b.billableCount())},
                new String[]{st.t("perf.notCharged"), number(Math.max(0, notCharged))},
                new String[]{st.t("perf.price"), price},
                new String[]{st.t("perf.amount"), money(totals.amountMinor(), totals.currency())},
                new String[]{st.t("perf.exceptions"), number(b.exceptionCount())});
        section(doc, st, n, st.t("sec.perf"), true, wrap(st, grid));
        return n + 1;
    }

    private void definitions(Document doc, ReportPdfStyle st, int n) throws DocumentException {
        PdfPTable t = table(st, 1.3f, 4.7f);
        for (String key : new String[]{"request", "success", "failed", "match", "exempt", "notRecorded", "latency", "time"}) {
            PdfPCell term = cell(st, st.t("def." + key + ".t"), st.bold(8.5f, NAVY));
            PdfPCell def = cell(st, st.t("def." + key + ".d"), st.font(8.5f, INK));
            for (PdfPCell c : new PdfPCell[]{term, def}) {
                c.setBorder(Rectangle.BOTTOM);
                c.setBorderColor(LINE);
                c.setBorderWidth(0.5f);
                c.setPaddingTop(5);
                c.setPaddingBottom(7);
            }
            t.addCell(term);
            t.addCell(def);
        }
        section(doc, st, n, st.t("sec.defs"), true, wrap(st, t));
    }

    // ----------------------------------------------------------------- details

    /**
     * Starts a per-transaction report. Feed rows with {@link DetailsWriter#addRows}
     * (in as many chunks as needed) and close it to finish the document.
     */
    public DetailsWriter openDetails(String tenantName, LocalDate from, LocalDate to, String statusFilter,
                                     ReportSummary.Totals totals, String lang, OutputStream out) {
        return new DetailsWriter(tenantName, from, to, statusFilter, totals, ReportPdfStyle.of(lang), out);
    }

    /**
     * Per-transaction PDF (A4 landscape). Completed rows are flushed to an
     * in-memory document as they are added; {@link #close()} adds the running
     * header / page numbers and writes the result to the output stream.
     */
    public static final class DetailsWriter implements AutoCloseable {

        private static final String[] COLUMNS = {
                "d.time", "d.txid", "d.type", "d.status", "d.verdict", "d.code", "d.reason",
                "d.device", "d.nfiq", "d.ms", "d.amount"};

        private final ReportPdfStyle st;
        private final String tenantName;
        private final OutputStream out;
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private final Document doc = new Document(PageSize.A4.rotate(), MARGIN_WIDE, MARGIN_WIDE, 56, 56);
        private final PdfPTable table;
        private final Font body;
        private long count;
        private long charged;
        private String currency = "";
        private boolean stripe;

        private DetailsWriter(String tenantName, LocalDate from, LocalDate to, String statusFilter,
                              ReportSummary.Totals totals, ReportPdfStyle st, OutputStream out) {
            this.st = st;
            this.tenantName = tenantName;
            this.out = out;
            this.body = st.font(7, INK);
            this.table = ReportPdfKit.table(st, 1.6f, 3.1f, 1.0f, 0.8f, 1.4f, 0.55f, 2.3f, 2.4f, 0.7f, 0.9f, 0.95f);
            try {
                PdfWriter writer = PdfWriter.getInstance(doc, buf);
                doc.open();
                float band = 84;
                coverBand(writer, doc, st, band, st.t("title.details"), tenantName,
                        st.range(from, to) + "   ·   " + st.t("report.details"));
                spacer(doc, band + 14 - doc.topMargin());
                PdfPTable info = infoGrid(st,
                        new String[]{st.t("info.client"), tenantName},
                        new String[]{st.t("info.period"), st.range(from, to)},
                        new String[]{st.t("info.statuses"), st.t("status." + statusKey(statusFilter))},
                        new String[]{st.t("info.generated"), st.generatedAt()},
                        new String[]{st.t("kpi.requests"), totals == null ? "—" : number(totals.totalTransactions())},
                        new String[]{st.t("kpi.amount"), totals == null ? "—" : money(totals.amountMinor(), totals.currency())});
                info.setSpacingAfter(14);
                doc.add(info);

                table.setHeaderRows(1);
                table.setComplete(false);
                for (String c : COLUMNS) {
                    boolean numeric = c.equals("d.nfiq") || c.equals("d.ms") || c.equals("d.amount");
                    PdfPCell h = numeric ? numberCell(st, st.t(c), st.bold(7, Color.WHITE)) : cell(st, st.t(c), st.bold(7, Color.WHITE));
                    h.setBackgroundColor(NAVY);
                    h.setPaddingTop(5);
                    h.setPaddingBottom(6);
                    h.setPaddingLeft(4);
                    h.setPaddingRight(4);
                    table.addCell(h);
                }
            } catch (DocumentException e) {
                doc.close();
                throw new RuntimeException("Failed to generate PDF report", e);
            }
        }

        public void addRows(List<ReportDetailRow> rows) {
            if (rows.isEmpty()) return;
            for (ReportDetailRow r : rows) {
                Color bg = stripe ? SOFT : Color.WHITE;
                row(cell(st, TIME.format(r.createdAt()), body), bg);
                row(cell(st, r.transactionId().toString(), st.font(7, MUTED)), bg);
                row(cell(st, st.t("type." + r.type()), body), bg);
                row(cell(st, st.status(r.status()), st.font(7, statusColor(r.status()))), bg);
                boolean noVerdict = r.verdict() == null && !"SUCCESS".equals(r.status());
                Color vc = verdictColor(r.verdict());
                row(cell(st, noVerdict ? "—" : st.verdict(r.verdict()), st.font(7, vc == GREY ? MUTED : vc)), bg);
                row(cell(st, r.errorCode() == null ? "" : String.valueOf(r.errorCode()), st.font(7, MUTED)), bg);
                row(cell(st, reason(st, r), body), bg);
                row(cell(st, r.deviceId() == null ? "" : r.deviceId(), body), bg);
                row(numberCell(st, r.imageNfiq2() == null ? "" : String.valueOf(r.imageNfiq2()), body), bg);
                row(numberCell(st, r.latencyMs() == null ? "" : number(r.latencyMs()), body), bg);
                row(numberCell(st, r.amountMinor() == null ? "—" : money(r.amountMinor(), r.currency()), body), bg);
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
                    text(doc, st, st.t("none"), st.font(10, MUTED), 4, 0);
                } else {
                    doc.add(table);
                    text(doc, st, st.fmt("d.total", "n", number(count), "amount", money(charged, currency)),
                            st.bold(9, NAVY), 10, 0);
                }
            } catch (DocumentException e) {
                throw new RuntimeException("Failed to generate PDF report", e);
            } finally {
                doc.close();
            }
            decorate(buf.toByteArray(), out, st, MARGIN_WIDE, st.t("title.details") + "  ·  " + tenantName, tenantName);
        }

        private void row(PdfPCell c, Color bg) {
            c.setBackgroundColor(bg);
            c.setPaddingTop(3.5f);
            c.setPaddingBottom(4.5f);
            c.setPaddingLeft(4);
            c.setPaddingRight(4);
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(LINE);
            c.setBorderWidth(0.4f);
            table.addCell(c);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static void headerRow(PdfPTable t, ReportPdfStyle st, Font font, boolean plain, String... keys) {
        for (String key : keys) {
            String label = key.isEmpty() ? "" : st.t(key);
            PdfPCell c = NUMERIC.contains(key) ? numberCell(st, label, font) : cell(st, label, font);
            if (plain) {
                c.setBorder(Rectangle.BOTTOM);
                c.setBorderColor(LINE);
                c.setBorderWidth(0.8f);
            } else {
                c.setBackgroundColor(NAVY);
            }
            c.setPaddingTop(5);
            c.setPaddingBottom(7);
            c.setPaddingLeft(7);
            c.setPaddingRight(7);
            t.addCell(c);
        }
    }

    private static final List<String> NUMERIC = List.of(
            "col.requests", "col.success", "col.failed", "col.rate", "col.amount", "col.count", "col.share");

    /** Adds one table row with a background colour and bottom hairlines. */
    private static void add(PdfPTable t, Color bg, PdfPCell... cells) {
        for (PdfPCell c : cells) {
            c.setBackgroundColor(bg);
            c.setPaddingTop(5);
            c.setPaddingBottom(7);
            c.setPaddingLeft(7);
            c.setPaddingRight(7);
            if (c.getBorder() == Rectangle.NO_BORDER) {
                c.setBorder(Rectangle.BOTTOM);
                c.setBorderColor(LINE);
                c.setBorderWidth(0.5f);
            }
            t.addCell(c);
        }
    }

    private static PdfPCell gap(float height) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setFixedHeight(height);
        return c;
    }

    private static long verdictCount(ReportSummary.Breakdown b, String verdict) {
        if (b == null) return 0;
        return b.verdicts().stream().filter(v -> verdict.equals(v.verdict()))
                .mapToLong(ReportSummary.VerdictCount::count).sum();
    }

    private static Color verdictColor(String verdict) {
        if (verdict == null) return GREY;
        return switch (verdict) {
            case "MATCH" -> GREEN;
            case "NO_MATCH" -> AMBER;
            case "EXEMPT" -> AZURE;
            case "NO_VERIFICATION_POSSIBLE" -> SKY;
            default -> GREY;
        };
    }

    private static Color statusColor(String status) {
        return switch (status) {
            case "SUCCESS" -> GREEN;
            case "REJECTED" -> AMBER;
            case "FAILED", "TIMEOUT" -> RED;
            default -> INK;
        };
    }

    private static String statusKey(String statusFilter) {
        if ("SUCCESS".equalsIgnoreCase(statusFilter)) return "SUCCESS";
        if ("FAILED".equalsIgnoreCase(statusFilter)) return "FAILED";
        return "ALL";
    }

    private static String reason(ReportPdfStyle st, ReportDetailRow r) {
        if (r.errorCode() != null || r.error() != null) return st.errorName(r.errorCode(), r.error());
        if (r.exceptionReason() != null) return st.t("exceptionPrefix") + st.exceptionReason(r.exceptionReason());
        return "";
    }
}
