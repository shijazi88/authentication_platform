package com.middleware.platform.transactions.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.middleware.platform.transactions.dto.ReportRow;
import com.middleware.platform.transactions.dto.ReportSummary;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.time.Instant;

/**
 * Renders a {@link ReportSummary} as a PDF document — suitable for
 * invoicing attachments, regulatory submissions, and email distribution.
 *
 * <p>Uses OpenPDF (LGPL fork of iText 4.2). No external fonts required.
 */
@Component
public class ReportPdfExporter {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 78, 140));
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
    private static final Font KPI_LABEL = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Font KPI_VALUE = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(30, 78, 140));
    private static final Color HEADER_BG = new Color(30, 78, 140);
    private static final Color STRIPE_BG = new Color(245, 247, 252);

    public void export(ReportSummary report, String tenantName, OutputStream out) {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Title
            Paragraph title = new Paragraph("Sannad — Transaction Report", TITLE_FONT);
            title.setSpacingAfter(4);
            doc.add(title);

            Paragraph subtitle = new Paragraph(
                    "Tenant: " + tenantName + "  |  " +
                            report.groupBy().substring(0, 1).toUpperCase() + report.groupBy().substring(1) +
                            "  |  " + report.from() + " → " + report.to() +
                            "  |  Generated: " + Instant.now().toString().substring(0, 16),
                    SUBTITLE_FONT);
            subtitle.setSpacingAfter(20);
            doc.add(subtitle);

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

            // Footer
            Paragraph footer = new Paragraph(
                    "\nThis report was generated automatically by the Sannad platform. "
                    + "For questions, contact integrations@sannad.example.",
                    new Font(Font.HELVETICA, 7, Font.ITALIC, Color.GRAY));
            footer.setSpacingBefore(24);
            doc.add(footer);

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        } finally {
            doc.close();
        }
    }

    private String formatMoney(long minor, String currency) {
        if ("YER".equals(currency)) {
            return minor / 100 + " " + currency;
        }
        return new DecimalFormat("#,##0.00").format(minor / 100.0) + " " + currency;
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
