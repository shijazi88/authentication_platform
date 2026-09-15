package com.middleware.platform.unit;

import com.middleware.platform.gateway.imagecheck.FingerprintImageValidator;
import com.middleware.platform.gateway.imagecheck.ImageFormat;
import com.middleware.platform.gateway.imagecheck.ImageInfo;
import com.middleware.platform.gateway.imagecheck.ImageValidationResult;
import com.middleware.platform.gateway.imagecheck.ImageValidationResult.Status;
import com.middleware.platform.gateway.imagecheck.ImageValidationSettings;
import com.middleware.platform.gateway.imagecheck.ImageValidationSettings.Mode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FingerprintImageValidatorTest {

    private final FingerprintImageValidator v = new FingerprintImageValidator(null);

    static ImageValidationSettings enforce() {
        ImageValidationSettings d = ImageValidationSettings.defaults();
        return new ImageValidationSettings(true, Mode.ENFORCE, d.allowedFormats(),
                d.minWidth(), d.minHeight(), d.maxWidth(), d.maxHeight(), d.requirePpi(), d.ppiMin(), d.ppiMax(),
                d.maxImageBytes(), d.requireGrayscale8Bit(), d.checkBlank(), d.minStdDev(), d.wsqMaxCompressionRatio(), true, 0.25);
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
                d.maxImageBytes(), true, true, d.minStdDev(), d.wsqMaxCompressionRatio(), true, 0.25);
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
                200, 200, 2000, 2000, false, 490, 510, 2_097_152, true, true, 10, 15, true, 0.25);
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
                .contains("indexed/palette").contains("re-save");
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
                d.maxImageBytes(), true, true, d.minStdDev(), d.wsqMaxCompressionRatio(), true, 0.25);
        assertThat(msg(noPpi, strict)).contains("does not declare its resolution");
    }

    @Test
    void settingsCrossFieldValidation() {
        assertThat(ImageValidationSettings.defaults().validationError()).isNull();
        ImageValidationSettings bad = new ImageValidationSettings(true, Mode.ENFORCE, Set.of(ImageFormat.PNG),
                900, 200, 500, 2000, false, 490, 510, 2_097_152, true, true, 10, 15, true, 0.25);
        assertThat(bad.validationError()).contains("minimum dimensions");
        ImageValidationSettings none = new ImageValidationSettings(true, Mode.ENFORCE, Set.of(),
                200, 200, 2000, 2000, false, 490, 510, 2_097_152, true, true, 10, 15, true, 0.25);
        assertThat(none.validationError()).contains("allowedFormats");
    }

    private String msg(ImageInfo i, ImageValidationSettings s) {
        ImageValidationResult r = v.validate(i, s);
        assertThat(r.status()).isEqualTo(Status.FAIL);
        return r.message();
    }
}
