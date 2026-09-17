package com.middleware.platform.unit;

import com.middleware.platform.gateway.imagecheck.FingerprintImageValidator;
import com.middleware.platform.gateway.imagecheck.ImageFormat;
import com.middleware.platform.gateway.imagecheck.ImageInfo;
import com.middleware.platform.gateway.imagecheck.ImageRejectedException;
import com.middleware.platform.gateway.imagecheck.ImageValidationResult;
import com.middleware.platform.gateway.imagecheck.ImageValidationResult.Status;
import com.middleware.platform.gateway.imagecheck.ImageValidationSettings;
import com.middleware.platform.gateway.imagecheck.ImageValidationSettings.Mode;
import com.middleware.platform.gateway.imagecheck.Nfiq2Client;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FingerprintImageValidatorTest {

    private final FingerprintImageValidator v = new FingerprintImageValidator(null, null);

    static ImageValidationSettings enforce() {
        ImageValidationSettings d = ImageValidationSettings.defaults();
        return new ImageValidationSettings(true, Mode.ENFORCE, d.allowedFormats(),
                d.minWidth(), d.minHeight(), d.maxWidth(), d.maxHeight(), d.requirePpi(), d.ppiMin(), d.ppiMax(),
                d.maxImageBytes(), d.requireGrayscale8Bit(), d.checkBlank(), d.minStdDev(), d.wsqMaxCompressionRatio(), true, 0.25, true, 40, true, Mode.ENFORCE, 5000, null, null, true);
    }

    static ImageInfo goodPng() { return new ImageInfo(ImageFormat.PNG, 90_000, 500, 500, 8, true, 500, null, 60.0, 0.8, 0, null); }
    static ImageInfo goodWsq() { return new ImageInfo(ImageFormat.WSQ, 30_000, 500, 500, 8, true, 500, 8.3, null, null, null, null); }

    @Test
    void goodImagesPass() {
        assertThat(v.validate(goodPng(), enforce()).status()).isEqualTo(Status.PASS);
        assertThat(v.validate(goodWsq(), enforce()).status()).isEqualTo(Status.PASS);
    }

    @Test
    void disabledSkips_andWarnModeNeverRejects() {
        ImageValidationSettings d = ImageValidationSettings.defaults();
        ImageValidationSettings off = new ImageValidationSettings(false, Mode.ENFORCE, d.allowedFormats(),
                d.minWidth(), d.minHeight(), d.maxWidth(), d.maxHeight(), false, d.ppiMin(), d.ppiMax(),
                d.maxImageBytes(), true, true, d.minStdDev(), d.wsqMaxCompressionRatio(), true, 0.25, true, 40, true, Mode.ENFORCE, 5000, null, null, true);
        assertThat(v.validate(ImageInfo.unreadable("image is empty"), off).status()).isEqualTo(Status.SKIP);

        ImageValidationResult r = v.validate(ImageInfo.unreadable("image is not valid base64"), d); // defaults = WARN
        assertThat(r.status()).isEqualTo(Status.WARN);
        assertThat(r.rejected()).isFalse();
        assertThat(r.message()).contains("base64");
    }

    @Test
    void eachRuleProducesAnActionableMessage() {
        ImageValidationSettings s = enforce();
        assertThat(msg(new ImageInfo(ImageFormat.UNKNOWN, 5000, null, null, null, null, null, null, null, null, null, null), s))
                .contains("unsupported image format").contains("PNG or WSQ");

        ImageValidationSettings pngOnly = new ImageValidationSettings(true, Mode.ENFORCE, Set.of(ImageFormat.PNG),
                200, 200, 2000, 2000, false, 490, 510, 2_097_152, true, true, 10, 15, true, 0.25, true, 40, true, Mode.ENFORCE, 5000, null, null, true);
        assertThat(msg(goodWsq(), pngOnly)).contains("WSQ images are not accepted").contains("allowed: PNG");

        assertThat(msg(new ImageInfo(ImageFormat.PNG, 3_000_000, 500, 500, 8, true, null, null, 60.0, 0.8, 0, null), s))
                .contains("exceeds the maximum 2.0 MB");
        assertThat(msg(new ImageInfo(ImageFormat.PNG, 9000, 120, 140, 8, true, null, null, 60.0, 0.8, 0, null), s))
                .contains("120×140").contains("minimum 200×200");
        assertThat(msg(new ImageInfo(ImageFormat.PNG, 9000, 2500, 500, 8, true, null, null, 60.0, 0.8, 0, null), s))
                .contains("exceeds the maximum 2000×2000");
        assertThat(msg(new ImageInfo(ImageFormat.PNG, 9000, 500, 500, 8, false, null, null, 60.0, 0.8, 2, null), s))
                .contains("8-bit greyscale").contains("RGB colour");
        // The Al-Qutaibi case (2026-09-14): indexed/palette PNG at 96 ppi → MOI ABIS 500 "number of image planes is invalid".
        assertThat(msg(new ImageInfo(ImageFormat.PNG, 86_364, 300, 375, 8, false, 96, null, 52.5, 0.84, 3, null), s))
                .contains("indexed/palette").contains("colour type 0");
        assertThat(msg(new ImageInfo(ImageFormat.PNG, 9000, 500, 500, 8, true, null, null, 60.0, 0.1, 0, null), s))
                .contains("too little fingerprint area").contains("10%").contains("25%");
        assertThat(msg(new ImageInfo(ImageFormat.PNG, 9000, 500, 500, 8, true, 300, null, 60.0, 0.8, 0, null), s))
                .contains("300 ppi").contains("490–510");
        assertThat(msg(new ImageInfo(ImageFormat.WSQ, 5000, 500, 500, 8, true, 500, 50.0, null, null, null, null), s))
                .contains("compression 50.0:1").contains("15:1");
        assertThat(msg(new ImageInfo(ImageFormat.PNG, 9000, 500, 500, 8, true, null, null, 1.2, 0.8, 0, null), s))
                .contains("blank or uniform");
        assertThat(msg(new ImageInfo(ImageFormat.PNG, 9000, 500, 500, 8, true, null, null, null, 0.8, 0, "PNG cannot be decoded"), s))
                .contains("cannot be decoded");
    }

    @Test
    void unknownPpiPassesUnlessRequired() {
        ImageInfo noPpi = new ImageInfo(ImageFormat.PNG, 9000, 500, 500, 8, true, null, null, 60.0, 0.8, 0, null);
        assertThat(v.validate(noPpi, enforce()).status()).isEqualTo(Status.PASS);
        ImageValidationSettings d = enforce();
        ImageValidationSettings strict = new ImageValidationSettings(true, Mode.ENFORCE, d.allowedFormats(),
                d.minWidth(), d.minHeight(), d.maxWidth(), d.maxHeight(), true, d.ppiMin(), d.ppiMax(),
                d.maxImageBytes(), true, true, d.minStdDev(), d.wsqMaxCompressionRatio(), true, 0.25, true, 40, true, Mode.ENFORCE, 5000, null, null, true);
        assertThat(msg(noPpi, strict)).contains("does not declare its resolution");
    }

    @Test
    void settingsCrossFieldValidation() {
        assertThat(ImageValidationSettings.defaults().validationError()).isNull();
        ImageValidationSettings bad = new ImageValidationSettings(true, Mode.ENFORCE, Set.of(ImageFormat.PNG),
                900, 200, 500, 2000, false, 490, 510, 2_097_152, true, true, 10, 15, true, 0.25, true, 40, true, Mode.ENFORCE, 5000, null, null, true);
        assertThat(bad.validationError()).contains("minimum dimensions");
        ImageValidationSettings none = new ImageValidationSettings(true, Mode.ENFORCE, Set.of(),
                200, 200, 2000, 2000, false, 490, 510, 2_097_152, true, true, 10, 15, true, 0.25, true, 40, true, Mode.ENFORCE, 5000, null, null, true);
        assertThat(none.validationError()).contains("allowedFormats");
    }

    private String msg(ImageInfo i, ImageValidationSettings s) {
        ImageValidationResult r = v.validate(i, s);
        assertThat(r.status()).isEqualTo(Status.FAIL);
        return r.message();
    }

    // ── NFIQ 2 branch (sidecar stubbed) ────────────────────────────────────

    static class StubNfiq2 extends Nfiq2Client {
        final Result result;
        StubNfiq2(Result r) { super("http://stub", 1000, new com.fasterxml.jackson.databind.ObjectMapper()); this.result = r; }
        @Override public Result score(byte[] image, ImageFormat format, int timeoutMs) { return result; }
    }

    static class StubSettings extends com.middleware.platform.common.settings.PlatformSettingsService {
        final ImageValidationSettings s;
        StubSettings(ImageValidationSettings s) { super(null, null); this.s = s; }
        @Override public <T> T get(String key, Class<T> type, java.util.function.Supplier<T> defaults) { return type.cast(s); }
    }

    static String goodPngBase64() throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(400, 400, java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
        java.util.Random rnd = new java.util.Random(3);
        for (int y = 0; y < 400; y++) for (int x = 0; x < 400; x++) { int g = rnd.nextInt(256); img.setRGB(x, y, (g << 16) | (g << 8) | g); }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return java.util.Base64.getEncoder().encodeToString(out.toByteArray());
    }

    @Test
    void nfiq2ScoreGatesAndIsRecorded() throws Exception {
        ImageValidationSettings s = enforce();
        String png = goodPngBase64();
        assertThat(new FingerprintImageValidator(new StubSettings(s), new StubNfiq2(new Nfiq2Client.Result(61, null, true, 120))).validate(png))
                .satisfies(r -> { assertThat(r.status()).isEqualTo(Status.PASS); assertThat(r.nfiq2Score()).isEqualTo(61); });
        ImageValidationResult low = new FingerprintImageValidator(new StubSettings(s), new StubNfiq2(new Nfiq2Client.Result(23, null, true, 120))).validate(png);
        assertThat(low.status()).isEqualTo(Status.FAIL);
        assertThat(low.message()).contains("NFIQ 2 score 23").contains("minimum 40");
        assertThat(low.nfiq2Score()).isEqualTo(23);
        ImageValidationResult unscorable = new FingerprintImageValidator(new StubSettings(s), new StubNfiq2(new Nfiq2Client.Result(null, "fingerprint area is too small", true, 90))).validate(png);
        assertThat(unscorable.status()).isEqualTo(Status.FAIL);
        assertThat(unscorable.message()).contains("too small");
        assertThat(unscorable.reason()).isEqualTo(ImageValidationResult.Reason.QUALITY);
        // sidecar down: fail-open → WARN, call continues
        ImageValidationResult down = new FingerprintImageValidator(new StubSettings(s), new StubNfiq2(Nfiq2Client.Result.unavailable("quality service unreachable"))).validate(png);
        assertThat(down.status()).isEqualTo(Status.WARN);
        assertThat(down.rejected()).isFalse();
        // structural failure short-circuits: the sidecar is never consulted
        ImageValidationResult garbage = new FingerprintImageValidator(new StubSettings(s), new StubNfiq2(new Nfiq2Client.Result(99, null, true, 1))).validate("QUJDREVGR0g=");
        assertThat(garbage.status()).isEqualTo(Status.FAIL);
        assertThat(garbage.nfiq2Score()).isNull();
        assertThat(garbage.reason()).isEqualTo(ImageValidationResult.Reason.FORMAT);
    }

    @Test
    void bankSpecificMinimum_andWarnOnlyScoreAction() throws Exception {
        ImageValidationSettings s = enforce();
        String png = goodPngBase64();
        StubNfiq2 scored35 = new StubNfiq2(new Nfiq2Client.Result(35, null, true, 100));
        // platform minimum 40 rejects 35; a bank override of 30 lets it through
        assertThat(new FingerprintImageValidator(new StubSettings(s), scored35).validate(png, null).status()).isEqualTo(Status.FAIL);
        assertThat(new FingerprintImageValidator(new StubSettings(s), scored35).validate(png, 30).status()).isEqualTo(Status.PASS);
        assertThat(new FingerprintImageValidator(new StubSettings(s), scored35).validate(png, 50).message()).contains("bank-specific");
        // score action WARN: structural rules still enforce, the score only records
        ImageValidationSettings scoreWarn = new ImageValidationSettings(true, Mode.ENFORCE, s.allowedFormats(),
                s.minWidth(), s.minHeight(), s.maxWidth(), s.maxHeight(), s.requirePpi(), s.ppiMin(), s.ppiMax(),
                s.maxImageBytes(), s.requireGrayscale8Bit(), s.checkBlank(), s.minStdDev(), s.wsqMaxCompressionRatio(), true, 0.25,
                true, 40, true, Mode.WARN, 5000, null, null, true);
        ImageValidationResult r = new FingerprintImageValidator(new StubSettings(scoreWarn), scored35).validate(png, null);
        assertThat(r.status()).isEqualTo(Status.WARN);
        assertThat(r.nfiq2Score()).isEqualTo(35);
        assertThat(new FingerprintImageValidator(new StubSettings(scoreWarn), scored35).validate("QUJDREVGR0g=").status()).isEqualTo(Status.FAIL);
    }

    @Test
    void imageRejectionCodesFollowTheReason() {
        assertThat(ImageRejectedException.codeFor(ImageValidationResult.Reason.FORMAT))
                .isEqualTo(com.middleware.platform.common.error.ErrorCode.IMAGE_FORMAT_REJECTED);
        assertThat(ImageRejectedException.codeFor(ImageValidationResult.Reason.QUALITY))
                .isEqualTo(com.middleware.platform.common.error.ErrorCode.IMAGE_QUALITY_REJECTED);
        assertThat(com.middleware.platform.common.error.ErrorCode.IMAGE_QUALITY_REJECTED.code()).isEqualTo(1003);
        assertThat(com.middleware.platform.common.error.ErrorCode.IMAGE_FORMAT_REJECTED.code()).isEqualTo(1004);
        ImageRejectedException ex = new ImageRejectedException(
                com.middleware.platform.common.error.ErrorCode.IMAGE_QUALITY_REJECTED, "Fingerprint image quality is not good.", 32);
        assertThat(ex.getErrorCode().status().value()).isEqualTo(400);
        assertThat(ex.getImageQuality()).isEqualTo(32);
    }

    @Test
    void plainBankMessages_andNormalisedDefaults() {
        ImageValidationSettings d = ImageValidationSettings.defaults();
        assertThat(d.bankMessage(ImageValidationResult.Reason.QUALITY)).isEqualTo(ImageValidationSettings.DEFAULT_QUALITY_MESSAGE)
                .doesNotContain("NFIQ").doesNotContain("ppi").doesNotContain("%");
        assertThat(d.bankMessage(ImageValidationResult.Reason.FORMAT)).isEqualTo(ImageValidationSettings.DEFAULT_FORMAT_MESSAGE);
        // a document saved before these fields existed (Jackson leaves them null / 0)
        ImageValidationSettings old = new ImageValidationSettings(true, Mode.ENFORCE, d.allowedFormats(),
                200, 200, 2000, 2000, false, 490, 510, 2_097_152, true, true, 10, 15, true, 0.25, true, 40, true,
                null, 0, null, "  ", null).normalized();
        assertThat(old.nfiq2Mode()).isEqualTo(Mode.ENFORCE);
        assertThat(old.nfiq2TimeoutMs()).isEqualTo(5000);
        assertThat(old.qualityMessage()).isEqualTo(ImageValidationSettings.DEFAULT_QUALITY_MESSAGE);
        assertThat(old.formatMessage()).isEqualTo(ImageValidationSettings.DEFAULT_FORMAT_MESSAGE);
        assertThat(old.returnScoreToBank()).isTrue();
    }
}
