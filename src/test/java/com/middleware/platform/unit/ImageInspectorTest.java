package com.middleware.platform.unit;

import com.middleware.platform.gateway.imagecheck.ImageFormat;
import com.middleware.platform.gateway.imagecheck.ImageInfo;
import com.middleware.platform.gateway.imagecheck.ImageInspector;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;

class ImageInspectorTest {

    // ── fixtures ────────────────────────────────────────────────────────────

    static byte[] png(int w, int h, int type, boolean noise) throws Exception {
        BufferedImage img = new BufferedImage(w, h, type);
        Random rnd = new Random(42);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int v = noise ? rnd.nextInt(256) : 200;
            img.setRGB(x, y, (v << 16) | (v << 8) | v);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /** Inserts a pHYs chunk (pixels per metre, unit=1) right after IHDR. */
    static byte[] withPhys(byte[] png, int ppi) {
        long ppm = Math.round(ppi / 0.0254);
        byte[] data = new byte[9];
        put32(data, 0, ppm); put32(data, 4, ppm); data[8] = 1;
        byte[] type = "pHYs".getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32(); crc.update(type); crc.update(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int ihdrEnd = 8 + 4 + 4 + 13 + 4;
        out.write(png, 0, ihdrEnd);
        byte[] len = new byte[4]; put32(len, 0, 9); out.writeBytes(len);
        out.writeBytes(type); out.writeBytes(data);
        byte[] c = new byte[4]; put32(c, 0, crc.getValue()); out.writeBytes(c);
        out.write(png, ihdrEnd, png.length - ihdrEnd);
        return out.toByteArray();
    }

    /** Minimal WSQ: SOI, NIST comment (with PPI), frame header (rows/cols), SOB + filler. */
    static byte[] wsq(int rows, int cols, Integer ppi, int totalBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xFF); out.write(0xA0);
        if (ppi != null) {
            byte[] txt = ("NIST_COM 9\nPIX_WIDTH " + cols + "\nPIX_HEIGHT " + rows + "\nPPI " + ppi + "\nLOSSY 1\n")
                    .getBytes(StandardCharsets.US_ASCII);
            out.write(0xFF); out.write(0xA8); out.write((txt.length + 2) >> 8); out.write((txt.length + 2) & 0xFF);
            out.writeBytes(txt);
        }
        out.write(0xFF); out.write(0xA2); out.write(0); out.write(17);   // Lf = 17
        out.write(0); out.write(255);                                    // black, white
        out.write(rows >> 8); out.write(rows & 0xFF);
        out.write(cols >> 8); out.write(cols & 0xFF);
        out.writeBytes(new byte[9]);                                     // m_shift, r_scale, encoder, software
        out.write(0xFF); out.write(0xA3); out.write(0); out.write(3); out.write(0); // SOB
        while (out.size() < totalBytes) out.write(0x5A);
        return out.toByteArray();
    }

    static String b64(byte[] b) { return Base64.getEncoder().encodeToString(b); }

    static void put32(byte[] b, int i, long v) {
        b[i] = (byte) (v >> 24); b[i + 1] = (byte) (v >> 16); b[i + 2] = (byte) (v >> 8); b[i + 3] = (byte) v;
    }

    // ── tests ───────────────────────────────────────────────────────────────

    @Test
    void greyscalePngWithNoiseIsReadCompletely() throws Exception {
        ImageInfo i = ImageInspector.inspect(b64(withPhys(png(400, 420, BufferedImage.TYPE_BYTE_GRAY, true), 500)));
        assertThat(i.format()).isEqualTo(ImageFormat.PNG);
        assertThat(i.width()).isEqualTo(400);
        assertThat(i.height()).isEqualTo(420);
        assertThat(i.bitDepth()).isEqualTo(8);
        assertThat(i.grayscale()).isTrue();
        assertThat(i.ppi()).isEqualTo(500);
        assertThat(i.stdDev()).isGreaterThan(50);
        assertThat(i.decodeError()).isNull();
    }

    @Test
    void uniformPngHasNoContrast_andRgbIsNotGreyscale() throws Exception {
        ImageInfo blank = ImageInspector.inspect(b64(png(300, 300, BufferedImage.TYPE_BYTE_GRAY, false)));
        assertThat(blank.stdDev()).isLessThan(1.0);
        assertThat(blank.ppi()).isNull();

        ImageInfo rgb = ImageInspector.inspect(b64(png(300, 300, BufferedImage.TYPE_INT_RGB, true)));
        assertThat(rgb.grayscale()).isFalse();
        assertThat(rgb.bitDepth()).isEqualTo(8);
    }

    @Test
    void dataUriPrefixAndWhitespaceAreTolerated() throws Exception {
        String raw = b64(png(250, 250, BufferedImage.TYPE_BYTE_GRAY, true));
        String wrapped = "data:image/png;base64," + raw.replaceAll("(.{76})", "$1\n");
        assertThat(ImageInspector.inspect(wrapped).format()).isEqualTo(ImageFormat.PNG);
    }

    @Test
    void wsqHeaderIsParsed() {
        ImageInfo i = ImageInspector.inspect(b64(wsq(500, 480, 500, 30_000)));
        assertThat(i.format()).isEqualTo(ImageFormat.WSQ);
        assertThat(i.height()).isEqualTo(500);
        assertThat(i.width()).isEqualTo(480);
        assertThat(i.ppi()).isEqualTo(500);
        assertThat(i.bitDepth()).isEqualTo(8);
        assertThat(i.compressionRatio()).isCloseTo(8.0, org.assertj.core.data.Offset.offset(0.1));
        assertThat(i.stdDev()).isNull();   // WSQ is not decoded
        assertThat(i.decodeError()).isNull();
    }

    @Test
    void wsqWithoutCommentHasUnknownPpi_andTruncatedWsqIsFlagged() {
        assertThat(ImageInspector.inspect(b64(wsq(500, 500, null, 20_000))).ppi()).isNull();
        byte[] cut = new byte[20];
        System.arraycopy(wsq(500, 500, 500, 1000), 0, cut, 0, 20);
        ImageInfo i = ImageInspector.inspect(b64(cut));
        assertThat(i.format()).isEqualTo(ImageFormat.WSQ);
        assertThat(i.decodeError()).contains("frame header");
    }

    @Test
    void garbageAndBadBase64AreUnknown() {
        assertThat(ImageInspector.inspect("%%%not base64%%%").decodeError()).contains("base64");
        ImageInfo jpeg = ImageInspector.inspect(b64(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        assertThat(jpeg.format()).isEqualTo(ImageFormat.UNKNOWN);
        assertThat(ImageInspector.inspect("").decodeError()).contains("empty");
    }
}
