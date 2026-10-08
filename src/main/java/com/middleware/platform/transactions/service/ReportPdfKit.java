package com.middleware.platform.transactions.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.middleware.platform.transactions.dto.ReportRow;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

import static com.middleware.platform.transactions.service.ReportPdfStyle.*;

/**
 * Layout building blocks shared by the report PDFs. Every piece of text sits
 * in a table cell (or ColumnText) with an explicit run direction — OpenPDF only
 * applies bidi ordering and Arabic shaping there — and every table is built in
 * logical (reading) order; for Arabic it is mirrored right-to-left.
 */
final class ReportPdfKit {

    private ReportPdfKit() {}

    private static final byte[] LOGO_WHITE = resource("branding/motabiq-logo-white.png");
    private static final byte[] LOGO_NAVY = resource("branding/motabiq-logo-navy.png");

    private static byte[] resource(String path) {
        try (InputStream in = ReportPdfKit.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing resource " + path);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Image logo(byte[] png, float height) {
        try {
            Image img = Image.getInstance(png);
            img.scaleToFit(height * 10, height);
            return img;
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Could not load logo", e);
        }
    }

    // ------------------------------------------------------------ primitives

    static PdfPTable table(ReportPdfStyle st, float... widths) {
        PdfPTable t = new PdfPTable(widths.length);
        t.setWidthPercentage(100);
        t.setRunDirection(st.direction());
        float[] w = widths.clone();
        if (st.rtl) { // RTL tables lay columns out right-to-left but apply widths left-to-right
            for (int i = 0, j = w.length - 1; i < j; i++, j--) { float x = w[i]; w[i] = w[j]; w[j] = x; }
        }
        try {
            t.setWidths(w);
        } catch (DocumentException e) {
            throw new IllegalArgumentException(e);
        }
        return t;
    }

    static PdfPCell cell(ReportPdfStyle st, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, font));
        c.setRunDirection(st.direction());
        c.setBorder(Rectangle.NO_BORDER);
        c.setLeading(0f, 1.25f);
        c.setPaddingTop(3);
        c.setPaddingBottom(5);
        return c;
    }

    /** Numbers line up at the end of the column (right in English, left in Arabic). */
    static PdfPCell numberCell(ReportPdfStyle st, String text, Font font) {
        PdfPCell c = cell(st, text, font);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    static PdfPCell wrap(ReportPdfStyle st, PdfPTable inner) {
        PdfPCell c = new PdfPCell(inner);
        c.setRunDirection(st.direction());
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(0);
        return c;
    }

    /** A line of text as a borderless one-cell table. */
    static void text(Document doc, ReportPdfStyle st, String text, Font font, float before, float after)
            throws DocumentException {
        PdfPTable t = table(st, 1);
        t.setSpacingBefore(before);
        t.setSpacingAfter(after);
        PdfPCell c = cell(st, text, font);
        c.setPadding(0);
        c.setPaddingBottom(2);
        t.addCell(c);
        doc.add(t);
    }

    static void spacer(Document doc, float height) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setFixedHeight(height);
        t.addCell(c);
        doc.add(t);
    }

    // -------------------------------------------------------------- sections

    /** "1  Executive summary" with a hairline underneath. */
    static PdfPCell sectionTitle(ReportPdfStyle st, int number, String title) {
        Phrase p = new Phrase();
        p.add(new Chunk(number + "   ", st.bold(12.5f, AZURE)));
        p.add(new Chunk(title, st.bold(12.5f, NAVY)));
        PdfPCell c = new PdfPCell(p);
        c.setRunDirection(st.direction());
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(LINE);
        c.setBorderWidth(0.8f);
        c.setPaddingTop(0);
        c.setPaddingBottom(6);
        return c;
    }

    /**
     * A numbered section: title plus content. {@code keepTogether} keeps short
     * sections on one page so a title is never stranded at the bottom.
     */
    static void section(Document doc, ReportPdfStyle st, int number, String title, boolean keepTogether,
                        PdfPCell... content) throws DocumentException {
        PdfPTable t = table(st, 1);
        t.setSpacingBefore(18);
        t.setKeepTogether(keepTogether);
        t.setSplitLate(false);
        t.addCell(sectionTitle(st, number, title));
        PdfPCell gap = new PdfPCell();
        gap.setBorder(Rectangle.NO_BORDER);
        gap.setFixedHeight(8);
        t.addCell(gap);
        for (PdfPCell c : content) t.addCell(c);
        doc.add(t);
    }

    static PdfPCell note(ReportPdfStyle st, String text) {
        PdfPCell c = cell(st, text, st.font(8, MUTED));
        c.setPaddingTop(5);
        return c;
    }

    // ---------------------------------------------------------- cover + info

    /** Navy band across the top of page 1: logo at the start side, title / client / period at the end side. */
    static void coverBand(PdfWriter writer, Document doc, ReportPdfStyle st, float height,
                          String title, String client, String detail) {
        PdfContentByte cb = writer.getDirectContent();
        Rectangle page = doc.getPageSize();
        float w = page.getWidth(), top = page.getHeight();
        cb.saveState();
        cb.setColorFill(NAVY);
        cb.rectangle(0, top - height, w, height);
        cb.fill();
        cb.setColorFill(AZURE);
        cb.rectangle(0, top - height - 3, w, 3);
        cb.fill();
        cb.restoreState();

        float margin = doc.leftMargin();
        Image img = logo(LOGO_WHITE, Math.min(38, height * 0.36f));
        float lx = st.rtl ? w - margin - img.getScaledWidth() : margin;
        img.setAbsolutePosition(lx, top - height / 2 - img.getScaledHeight() / 2);
        try {
            cb.addImage(img);
        } catch (DocumentException e) {
            throw new IllegalStateException(e);
        }

        float tx = st.rtl ? margin : w - margin;
        int align = st.rtl ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT;
        float mid = top - height / 2;
        show(cb, st, align, title, st.bold(17, Color.WHITE), tx, mid + 9);
        show(cb, st, align, client, st.font(10.5f, Color.WHITE), tx, mid - 10);
        show(cb, st, align, detail, st.font(9, SKY), tx, mid - 25);
    }

    private static void show(PdfContentByte cb, ReportPdfStyle st, int align, String text, Font font, float x, float y) {
        ColumnText.showTextAligned(cb, align, new Phrase(text, font), x, y, 0, st.direction(), 0);
    }

    /** Label/value pairs, two pairs per row, on a soft panel. */
    static PdfPTable infoGrid(ReportPdfStyle st, String[]... pairs) {
        PdfPTable t = table(st, 1.5f, 2.0f, 1.5f, 2.0f);
        for (int i = 0; i < pairs.length; i += 2) {
            for (int j = i; j < i + 2; j++) {
                String[] pair = j < pairs.length ? pairs[j] : new String[]{"", ""};
                PdfPCell k = cell(st, pair[0], st.font(8, MUTED));
                PdfPCell v = cell(st, pair[1], st.font(9.5f, INK));
                for (PdfPCell c : new PdfPCell[]{k, v}) {
                    c.setBackgroundColor(SOFT);
                    c.setPaddingTop(6);
                    c.setPaddingBottom(7);
                    c.setPaddingLeft(10);
                    c.setPaddingRight(10);
                    if (i + 2 < pairs.length) {
                        c.setBorder(Rectangle.BOTTOM);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(1.2f);
                    }
                }
                t.addCell(k);
                t.addCell(v);
            }
        }
        return t;
    }

    // ------------------------------------------------------------------ KPIs

    static PdfPCell kpi(ReportPdfStyle st, String label, String value, String caption, Color accent) {
        PdfPCell c = new PdfPCell();
        c.setRunDirection(st.direction());
        c.setBorder(Rectangle.TOP);
        c.setBorderWidthTop(2.5f);
        c.setBorderColorTop(accent);
        c.setBackgroundColor(SOFT);
        c.setPaddingTop(9);
        c.setPaddingBottom(11);
        c.setPaddingLeft(11);
        c.setPaddingRight(11);
        Paragraph l = new Paragraph(label, st.font(8.5f, MUTED));
        l.setLeading(0, 1.2f);
        Paragraph v = new Paragraph(value, st.bold(19, NAVY));
        v.setLeading(0, 1.25f);
        v.setSpacingBefore(2);
        c.addElement(l);
        c.addElement(v);
        if (caption != null && !caption.isEmpty()) {
            Paragraph cap = new Paragraph(caption, st.font(8, MUTED));
            cap.setLeading(0, 1.2f);
            cap.setSpacingBefore(4);
            c.addElement(cap);
        }
        return c;
    }

    /** Gap cell between KPI cards. */
    static PdfPCell gutter() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    // ------------------------------------------------------------------ bars

    /** A horizontal bar (on a light track) filling {@code fraction} of the cell, growing from the start side. */
    static PdfPCell barCell(ReportPdfStyle st, double fraction, Color color) {
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.NO_BORDER);
        c.setCellEvent(new Bar(Math.max(0, Math.min(1, fraction)), color, st.rtl));
        return c;
    }

    private record Bar(double fraction, Color color, boolean rtl) implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle r, PdfContentByte[] canvases) {
            PdfContentByte cb = canvases[PdfPTable.BACKGROUNDCANVAS];
            float h = 7, x0 = r.getLeft() + 4, x1 = r.getRight() - 4, w = x1 - x0;
            float y = (r.getTop() + r.getBottom()) / 2 - h / 2;
            cb.saveState();
            cb.setColorFill(TRACK);
            cb.roundRectangle(x0, y, w, h, h / 2);
            cb.fill();
            float bw = (float) (w * fraction);
            if (bw > 0) {
                bw = Math.max(bw, h);
                cb.setColorFill(color);
                cb.roundRectangle(rtl ? x1 - bw : x0, y, bw, h, h / 2);
                cb.fill();
            }
            cb.restoreState();
        }
    }

    // ----------------------------------------------------------------- trend

    /** Stacked columns (successful / failed) per period, chronological left to right. */
    static PdfPCell trendCell(ReportPdfStyle st, List<ReportRow> rows, float height) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setFixedHeight(height);
        c.setCellEvent(new Trend(st, rows));
        return c;
    }

    private record Trend(ReportPdfStyle st, List<ReportRow> rows) implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle r, PdfContentByte[] canvases) {
            PdfContentByte cb = canvases[PdfPTable.LINECANVAS];
            float left = r.getLeft() + 30, right = r.getRight() - 4;
            float bottom = r.getBottom() + 20, top = r.getTop() - 24;
            long max = rows.stream().mapToLong(ReportRow::total).max().orElse(0);
            long step = niceStep(max);
            long yMax = step * 4;
            Font axis = st.font(7, MUTED);

            cb.saveState();
            cb.setLineWidth(0.5f);
            for (int i = 0; i <= 4; i++) {
                float y = bottom + (top - bottom) * i / 4f;
                cb.setColorStroke(i == 0 ? GREY : LINE);
                cb.moveTo(left, y);
                cb.lineTo(right, y);
                cb.stroke();
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(number(step * i), axis),
                        left - 5, y - 2.5f, 0);
            }

            int n = rows.size();
            float slot = (right - left) / Math.max(1, n);
            float bw = Math.min(slot * 0.62f, 30);
            int every = (int) Math.ceil(n / 12.0);
            for (int i = 0; i < n; i++) {
                ReportRow row = rows.get(i);
                float x = left + slot * i + (slot - bw) / 2;
                float hs = (float) row.successCount() / yMax * (top - bottom);
                float hf = (float) row.failedCount() / yMax * (top - bottom);
                if (hs > 0) {
                    cb.setColorFill(GREEN);
                    cb.rectangle(x, bottom, bw, hs);
                    cb.fill();
                }
                if (hf > 0) {
                    cb.setColorFill(RED);
                    cb.rectangle(x, bottom + hs, bw, hf);
                    cb.fill();
                }
                if (i % every == 0) {
                    ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                            new Phrase(st.periodShort(row.period()), axis), x + bw / 2, bottom - 11, 0,
                            st.direction(), 0);
                }
            }

            // legend at the start side, above the plot
            float ly = r.getTop() - 10;
            float lx = st.rtl ? right : left;
            for (Object[] item : new Object[][]{{GREEN, st.t("chart.success")}, {RED, st.t("chart.failed")}}) {
                Color col = (Color) item[0];
                String label = (String) item[1];
                float sq = 7;
                cb.setColorFill(col);
                cb.rectangle(st.rtl ? lx - sq : lx, ly - 1, sq, sq);
                cb.fill();
                float tx = st.rtl ? lx - sq - 4 : lx + sq + 4;
                Phrase p = new Phrase(label, st.font(8, INK));
                ColumnText.showTextAligned(cb, st.rtl ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT, p, tx, ly, 0,
                        st.direction(), 0);
                float tw = ColumnText.getWidth(p, st.direction(), 0);
                lx = st.rtl ? tx - tw - 14 : tx + tw + 14;
            }
            cb.restoreState();
        }

        /** Gridline step so that four steps cover {@code max}: 1, 2, 5 × 10^k. */
        private static long niceStep(long max) {
            if (max <= 4) return 1;
            double raw = max / 4.0;
            double mag = Math.pow(10, Math.floor(Math.log10(raw)));
            double f = raw / mag;
            double nice = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
            return Math.max(1, (long) Math.ceil(nice * mag));
        }
    }

    // ------------------------------------------------- running header/footer

    /**
     * Second pass over the finished PDF: a running header (logo + report title)
     * on pages 2+, and on every page a footer with the confidentiality line,
     * the brand and "Page i of n" — the total is only known at this point.
     */
    static void decorate(byte[] pdf, OutputStream out, ReportPdfStyle st, float margin,
                         String runningTitle, String client) {
        try {
            PdfReader reader = new PdfReader(pdf);
            PdfStamper stamper = new PdfStamper(reader, out);
            int n = reader.getNumberOfPages();
            Font small = st.font(7.5f, MUTED);
            for (int i = 1; i <= n; i++) {
                Rectangle page = reader.getPageSizeWithRotation(i);
                float w = page.getWidth(), h = page.getHeight();
                float start = st.rtl ? w - margin : margin, end = st.rtl ? margin : w - margin;
                int startAlign = st.rtl ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT;
                int endAlign = st.rtl ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT;
                PdfContentByte cb = stamper.getOverContent(i);

                if (i > 1) {
                    Image img = logo(LOGO_NAVY, 13);
                    img.setAbsolutePosition(st.rtl ? start - img.getScaledWidth() : start, h - 34);
                    cb.addImage(img);
                    show(cb, st, endAlign, runningTitle, small, end, h - 30);
                    hairline(cb, margin, w - margin, h - 42);
                }

                hairline(cb, margin, w - margin, 40);
                show(cb, st, startAlign, st.fmt("footer.confidential", "client", client), small, start, 27);
                show(cb, st, Element.ALIGN_CENTER, st.t("footer.brand"), small, w / 2, 27);
                show(cb, st, endAlign, st.fmt("page", "i", String.valueOf(i), "n", String.valueOf(n)), small, end, 27);
            }
            stamper.close();
            reader.close();
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Failed to finish PDF report", e);
        }
    }

    private static void hairline(PdfContentByte cb, float x0, float x1, float y) {
        cb.saveState();
        cb.setColorStroke(LINE);
        cb.setLineWidth(0.6f);
        cb.moveTo(x0, y);
        cb.lineTo(x1, y);
        cb.stroke();
        cb.restoreState();
    }
}
