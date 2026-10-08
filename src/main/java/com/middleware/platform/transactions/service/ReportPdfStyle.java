package com.middleware.platform.transactions.service;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * Brand palette, fonts, wording and number/date formatting for the report PDFs,
 * in English or Arabic. Both languages embed IBM Plex Sans Arabic (Latin +
 * Arabic, SIL OFL — resources/fonts/ibm-plex-sans-arabic): OpenPDF shapes
 * Arabic into the Unicode presentation forms, so the font must map all of them
 * (Cairo, the portal's web font, does not — letters went missing).
 */
final class ReportPdfStyle {

    // MOTABIQ palette (portal-admin tailwind.config.js)
    static final Color NAVY = new Color(0x00, 0x3b, 0x73);
    static final Color AZURE = new Color(0x2f, 0x7f, 0xc9);
    static final Color SKY = new Color(0x7f, 0xb3, 0xe6);
    static final Color GREEN = new Color(0x1f, 0x7a, 0x4d);
    static final Color RED = new Color(0xc8, 0x3a, 0x3a);
    static final Color AMBER = new Color(0xd9, 0x77, 0x06);
    static final Color INK = new Color(0x1a, 0x1f, 0x2b);
    static final Color MUTED = new Color(0x5b, 0x60, 0x76);
    static final Color LINE = new Color(0xe3, 0xe6, 0xee);
    static final Color SOFT = new Color(0xf5, 0xf7, 0xfb);
    static final Color TRACK = new Color(0xe9, 0xed, 0xf4);
    static final Color GREY = new Color(0xb4, 0xbb, 0xc8);

    final boolean rtl;
    private final Map<String, String> labels;
    private final BaseFont regular;
    private final BaseFont bold;

    private ReportPdfStyle(boolean rtl, Map<String, String> labels, BaseFont regular, BaseFont bold) {
        this.rtl = rtl;
        this.labels = labels;
        this.regular = regular;
        this.bold = bold;
    }

    static ReportPdfStyle of(String lang) {
        return "ar".equalsIgnoreCase(lang) ? Holder.AR : Holder.EN;
    }

    /** Lazily built once; both languages embed IBM Plex Sans Arabic. */
    private static final class Holder {
        static final BaseFont REGULAR = loadFont("fonts/ibm-plex-sans-arabic/IBMPlexSansArabic-Regular.ttf");
        static final BaseFont BOLD = loadFont("fonts/ibm-plex-sans-arabic/IBMPlexSansArabic-Bold.ttf");
        static final ReportPdfStyle EN = new ReportPdfStyle(false, ReportPdfLabels.EN, REGULAR, BOLD);
        static final ReportPdfStyle AR = new ReportPdfStyle(true, ReportPdfLabels.AR, REGULAR, BOLD);
    }

    private static BaseFont loadFont(String resource) {
        try (InputStream in = ReportPdfStyle.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("Missing font resource " + resource);
            return BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load font " + resource, e);
        }
    }

    Font font(float size, Color color) {
        return new Font(regular, size, Font.NORMAL, color);
    }

    Font bold(float size, Color color) {
        return new Font(bold, size, Font.NORMAL, color);
    }

    /** OpenPDF run direction: bidi with an RTL or LTR base, never NO_BIDI (that skips Arabic shaping). */
    int direction() {
        return rtl ? PdfWriter.RUN_DIRECTION_RTL : PdfWriter.RUN_DIRECTION_LTR;
    }

    // ---------------------------------------------------------------- wording

    String t(String key) {
        return labels.getOrDefault(key, key);
    }

    /** {@code t(key)} with {name} placeholders filled in pairs: fmt("k", "n", "3", "x", "y"). */
    String fmt(String key, String... pairs) {
        String s = t(key);
        for (int i = 0; i + 1 < pairs.length; i += 2) s = s.replace("{" + pairs[i] + "}", pairs[i + 1]);
        return s;
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

    /** Plain error name — the Support page's playbook wording; falls back to the enum name. */
    String errorName(Integer code, String error) {
        String named = code == null ? null : labels.get("err." + code);
        return named != null ? named : humanize(error);
    }

    /** IMAGE_QUALITY_REJECTED → "Image quality rejected". */
    static String humanize(String enumName) {
        if (enumName == null || enumName.isEmpty()) return "";
        String s = enumName.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------- formatting

    private static final DecimalFormatSymbols US = DecimalFormatSymbols.getInstance(Locale.US);

    static String number(long n) {
        return new DecimalFormat("#,##0", US).format(n);
    }

    static String percent(double fraction) {
        return new DecimalFormat("#0.0", US).format(fraction * 100) + "%";
    }

    static String share(long part, long whole) {
        return whole == 0 ? "—" : percent((double) part / whole);
    }

    static String money(long minor, String currency) {
        String cur = currency == null || currency.isEmpty() ? "YER" : currency;
        if ("YER".equals(cur)) return number(minor / 100) + " " + cur;
        return new DecimalFormat("#,##0.00", US).format(minor / 100.0) + " " + cur;
    }

    /** 2026-10-02 → "2 Oct 2026" / "2 أكتوبر 2026". */
    String date(LocalDate d) {
        return d.getDayOfMonth() + " " + month(d.getMonthValue(), !rtl) + " " + d.getYear();
    }

    /** 2026-10-02 → "2 Oct" / "2 أكتوبر" (chart axis). */
    String dayShort(LocalDate d) {
        return d.getDayOfMonth() + " " + month(d.getMonthValue(), true);
    }

    /** "2026-09" → "Sep 2026" / "سبتمبر 2026"; "2026-10-02" → date(). */
    String period(String period) {
        if (period.length() == 7) {
            return month(Integer.parseInt(period.substring(5, 7)), !rtl) + " " + period.substring(0, 4);
        }
        return date(LocalDate.parse(period));
    }

    /** Shorter period label for chart axes. */
    String periodShort(String period) {
        if (period.length() == 7) return month(Integer.parseInt(period.substring(5, 7)), true) + " " + period.substring(2, 4);
        return dayShort(LocalDate.parse(period));
    }

    String range(LocalDate from, LocalDate to) {
        return fmt("range", "from", date(from), "to", date(to));
    }

    String generatedAt() {
        ZonedDateTime now = Instant.now().atZone(ZoneOffset.UTC);
        return fmt("generatedAt", "date", date(now.toLocalDate()),
                "time", String.format(Locale.US, "%02d:%02d", now.getHour(), now.getMinute()));
    }

    private String month(int m, boolean shortForm) {
        return t((shortForm ? "mon." : "month.") + m);
    }
}
