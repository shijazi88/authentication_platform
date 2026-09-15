package com.middleware.platform.gateway.imagecheck;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java structural inspection of a base64 fingerprint image. Reads only
 * headers (plus pixel statistics for PNG); nothing is written anywhere and the
 * decoded bytes are dropped on return. Never throws — problems are reported in
 * {@link ImageInfo#decodeError()} so the caller decides what to do.
 */
public final class ImageInspector {

    private static final byte[] PNG_SIG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int WSQ_SOI = 0xFFA0, WSQ_EOI = 0xFFA1, WSQ_SOF = 0xFFA2,
            WSQ_SOB = 0xFFA3, WSQ_COM = 0xFFA8;
    private static final Pattern WSQ_PPI = Pattern.compile("(?im)^\\s*PPI\\s+(\\d{2,5})\\s*$");
    private static final Pattern DATA_URI = Pattern.compile("^data:[^,]*,", Pattern.CASE_INSENSITIVE);
    /** Upper bound on pixels sampled for the blank-image statistic. */
    private static final int MAX_SAMPLES = 16_384;

    private ImageInspector() {}

    public static ImageInfo inspect(String base64) {
        if (base64 == null || base64.isBlank()) return ImageInfo.unreadable("image is empty");
        String b64 = DATA_URI.matcher(base64.trim()).replaceFirst("");
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(b64);
        } catch (IllegalArgumentException ex) {
            return ImageInfo.unreadable("image is not valid base64");
        }
        if (bytes.length < 16) return ImageInfo.unreadable("image is too small to be a fingerprint image");

        if (startsWith(bytes, PNG_SIG)) return inspectPng(bytes);
        if (u16(bytes, 0) == WSQ_SOI) return inspectWsq(bytes);
        return new ImageInfo(ImageFormat.UNKNOWN, bytes.length, null, null, null, null, null, null, null, null);
    }

    // ── PNG ─────────────────────────────────────────────────────────────────

    private static ImageInfo inspectPng(byte[] b) {
        Integer width = null, height = null, bitDepth = null, ppi = null;
        Boolean gray = null;
        int pos = 8;
        try {
            while (pos + 8 <= b.length) {
                int len = (int) u32(b, pos);
                String type = new String(b, pos + 4, 4, StandardCharsets.US_ASCII);
                int data = pos + 8;
                if (len < 0 || data + len > b.length) break;
                switch (type) {
                    case "IHDR" -> {
                        width = (int) u32(b, data);
                        height = (int) u32(b, data + 4);
                        bitDepth = b[data + 8] & 0xFF;
                        int colorType = b[data + 9] & 0xFF;
                        gray = colorType == 0 || colorType == 4;
                    }
                    case "pHYs" -> {
                        long xppm = u32(b, data);
                        int unit = b[data + 8] & 0xFF;
                        if (unit == 1 && xppm > 0) ppi = (int) Math.round(xppm * 0.0254);
                    }
                    case "IDAT", "IEND" -> { /* header chunks all precede IDAT */ }
                    default -> { }
                }
                if (type.equals("IDAT") || type.equals("IEND")) break;
                pos = data + len + 4; // + CRC
            }
        } catch (RuntimeException ex) {
            return new ImageInfo(ImageFormat.PNG, b.length, width, height, bitDepth, gray, ppi, null, null,
                    "PNG header is corrupt");
        }
        if (width == null) {
            return new ImageInfo(ImageFormat.PNG, b.length, null, null, null, null, null, null, null,
                    "PNG has no IHDR chunk");
        }

        Double stdDev = null;
        String decodeError = null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(b));
            if (img == null) decodeError = "PNG cannot be decoded";
            else stdDev = grayStdDev(img.getRaster());
        } catch (Exception ex) {
            decodeError = "PNG cannot be decoded";
        }
        return new ImageInfo(ImageFormat.PNG, b.length, width, height, bitDepth, gray, ppi, null, stdDev, decodeError);
    }

    /** Standard deviation of band 0 over a regular sample grid — cheap blank/uniform detector. */
    static double grayStdDev(Raster r) {
        int w = r.getWidth(), h = r.getHeight();
        int step = Math.max(1, (int) Math.ceil(Math.sqrt((double) w * h / MAX_SAMPLES)));
        double sum = 0, sumSq = 0;
        long n = 0;
        int maxVal = (1 << r.getSampleModel().getSampleSize(0)) - 1;
        double scale = maxVal > 0 ? 255.0 / maxVal : 1.0;
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                double v = r.getSample(x, y, 0) * scale;
                sum += v; sumSq += v * v; n++;
            }
        }
        if (n == 0) return 0;
        double mean = sum / n;
        return Math.sqrt(Math.max(0, sumSq / n - mean * mean));
    }

    // ── WSQ ─────────────────────────────────────────────────────────────────

    private static ImageInfo inspectWsq(byte[] b) {
        Integer rows = null, cols = null, ppi = null;
        int pos = 2;
        try {
            while (pos + 4 <= b.length) {
                int marker = u16(b, pos);
                if ((marker & 0xFF00) != 0xFF00) {
                    return new ImageInfo(ImageFormat.WSQ, b.length, cols, rows, 8, true, ppi, null, null,
                            "WSQ marker sequence is corrupt");
                }
                if (marker == WSQ_EOI) break;
                int len = u16(b, pos + 2);           // segment length includes the 2 length bytes
                int data = pos + 4;
                if (len < 2 || pos + 2 + len > b.length) break;
                if (marker == WSQ_SOF) {
                    // Lf(2) black(1) white(1) rows(2) cols(2) ...
                    rows = u16(b, data + 2);
                    cols = u16(b, data + 4);
                } else if (marker == WSQ_COM) {
                    String text = new String(b, data, len - 2, StandardCharsets.ISO_8859_1);
                    Matcher m = WSQ_PPI.matcher(text);
                    if (m.find()) ppi = Integer.parseInt(m.group(1));
                }
                if (marker == WSQ_SOB) break;         // entropy-coded data follows
                if (rows != null && ppi != null) break;
                pos = pos + 2 + len;
            }
        } catch (RuntimeException ex) {
            return new ImageInfo(ImageFormat.WSQ, b.length, cols, rows, 8, true, ppi, null, null,
                    "WSQ header is corrupt");
        }
        if (rows == null || cols == null) {
            return new ImageInfo(ImageFormat.WSQ, b.length, null, null, 8, true, ppi, null, null,
                    "WSQ has no frame header");
        }
        double ratio = (double) rows * cols / b.length;
        return new ImageInfo(ImageFormat.WSQ, b.length, cols, rows, 8, true, ppi, ratio, null, null);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static boolean startsWith(byte[] b, byte[] sig) {
        if (b.length < sig.length) return false;
        for (int i = 0; i < sig.length; i++) if (b[i] != sig[i]) return false;
        return true;
    }

    private static int u16(byte[] b, int i) { return ((b[i] & 0xFF) << 8) | (b[i + 1] & 0xFF); }

    private static long u32(byte[] b, int i) {
        return ((long) (b[i] & 0xFF) << 24) | ((b[i + 1] & 0xFF) << 16) | ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
    }
}
