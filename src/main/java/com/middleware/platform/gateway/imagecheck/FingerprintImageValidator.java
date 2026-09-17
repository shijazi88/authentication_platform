package com.middleware.platform.gateway.imagecheck;

import com.middleware.platform.common.settings.PlatformSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.middleware.platform.gateway.imagecheck.ImageValidationResult.Reason;
import static com.middleware.platform.gateway.imagecheck.ImageValidationResult.Status;

/**
 * Applies the admin-configured {@link ImageValidationSettings} to a fingerprint
 * image. Rules are evaluated in order of cost and only the first failure is
 * reported. The detail is technical and stays on the transaction for staff;
 * the bank receives one of the two plain sentences from the settings, chosen
 * by {@link Reason}.
 */
@Component
@RequiredArgsConstructor
public class FingerprintImageValidator {

    private final PlatformSettingsService settings;
    private final Nfiq2Client nfiq2;

    public ImageValidationSettings currentSettings() {
        return settings.get(ImageValidationSettings.KEY, ImageValidationSettings.class,
                ImageValidationSettings::defaults).normalized();
    }

    /** A failed rule: the technical detail (for staff) and the category (chooses the bank's sentence). */
    record Problem(Reason reason, String detail) {}

    /**
     * Inspects and validates a base64 image with the current runtime settings:
     * structural rules first (cheap, in-process), then — only if those pass and
     * the rule is on — the NFIQ 2 quality score from the sidecar.
     */
    public ImageValidationResult validate(String base64) {
        return validate(base64, null);
    }

    /**
     * @param tenantMinNfiq2 the bank's own minimum score, or null for the platform default
     */
    public ImageValidationResult validate(String base64, Integer tenantMinNfiq2) {
        ImageValidationSettings s = currentSettings();
        byte[] raw = ImageInspector.decode(base64);
        ImageInfo info = raw == null ? ImageInspector.inspect(base64) : ImageInspector.inspect(raw);
        ImageValidationResult structural = validate(info, s);
        if (structural.status() != Status.PASS || !s.checkNfiq2() || raw == null || nfiq2 == null
                || info.format() == ImageFormat.UNKNOWN) {
            return structural;
        }
        Nfiq2Client.Result q = nfiq2.score(raw, info.format(), s.nfiq2TimeoutMs());
        // The score has its own action so an admin can observe scores while the
        // structural rules stay enforced.
        boolean enforce = s.mode() == ImageValidationSettings.Mode.ENFORCE
                && s.nfiq2Mode() == ImageValidationSettings.Mode.ENFORCE;
        Status onProblem = enforce ? Status.FAIL : Status.WARN;
        if (!q.available()) {
            String msg = "fingerprint quality could not be measured (" + q.error() + ")";
            return new ImageValidationResult(s.nfiq2FailOpen() ? Status.WARN : onProblem, msg, info, null, Reason.SERVICE);
        }
        if (q.score() == null) {
            return new ImageValidationResult(onProblem,
                    "fingerprint quality could not be measured: " + q.error(), info, null, Reason.QUALITY);
        }
        int min = tenantMinNfiq2 != null ? tenantMinNfiq2 : s.minNfiq2();
        if (q.score() < min) {
            return new ImageValidationResult(onProblem, String.format(Locale.ROOT,
                    "fingerprint quality too low (NFIQ 2 score %d, minimum %d%s)",
                    q.score(), min, tenantMinNfiq2 != null ? ", bank-specific" : ""), info, q.score(), Reason.QUALITY);
        }
        return new ImageValidationResult(Status.PASS, null, info, q.score(), Reason.NONE);
    }

    public ImageValidationResult validate(ImageInfo info, ImageValidationSettings s) {
        if (!s.enabled()) return new ImageValidationResult(Status.SKIP, null, info);
        Problem problem = firstProblem(info, s);
        if (problem == null) return new ImageValidationResult(Status.PASS, null, info, null, Reason.NONE);
        Status st = s.mode() == ImageValidationSettings.Mode.ENFORCE ? Status.FAIL : Status.WARN;
        return new ImageValidationResult(st, problem.detail(), info, null, problem.reason());
    }

    static Problem firstProblem(ImageInfo info, ImageValidationSettings s) {
        String f = firstFormatProblem(info, s);
        if (f != null) return new Problem(Reason.FORMAT, f);
        String q = firstQualityProblem(info, s);
        return q == null ? null : new Problem(Reason.QUALITY, q);
    }

    /** Container / encoding / size / resolution rules — the bank's integration must change. */
    static String firstFormatProblem(ImageInfo info, ImageValidationSettings s) {
        if (info.format() == ImageFormat.UNKNOWN) {
            return info.decodeError() != null ? info.decodeError()
                    : "unsupported image format (expected " + formats(s) + ")";
        }
        if (!s.allowedFormats().contains(info.format())) {
            return info.format() + " images are not accepted (allowed: " + formats(s) + ")";
        }
        if (info.bytes() > s.maxImageBytes()) {
            return String.format(Locale.ROOT, "image size %s exceeds the maximum %s",
                    mb(info.bytes()), mb(s.maxImageBytes()));
        }
        if (info.decodeError() != null) return info.decodeError();
        if (info.width() != null && info.height() != null) {
            if (info.width() < s.minWidth() || info.height() < s.minHeight()) {
                return String.format(Locale.ROOT, "image %d×%d px is below the minimum %d×%d px",
                        info.width(), info.height(), s.minWidth(), s.minHeight());
            }
            if (info.width() > s.maxWidth() || info.height() > s.maxHeight()) {
                return String.format(Locale.ROOT, "image %d×%d px exceeds the maximum %d×%d px",
                        info.width(), info.height(), s.maxWidth(), s.maxHeight());
            }
        }
        if (s.requireGrayscale8Bit() && info.format() == ImageFormat.PNG
                && (Boolean.FALSE.equals(info.grayscale()) || (info.bitDepth() != null && info.bitDepth() != 8))) {
            // Name the actual encoding: the identity provider's matcher only accepts a
            // single-plane image, and an indexed/palette PNG is the usual culprit.
            return "PNG must be 8-bit greyscale (colour type 0); got "
                    + (info.bitDepth() != null ? info.bitDepth() + "-bit " : "")
                    + info.colorTypeName();
        }
        if (info.ppi() != null) {
            if (info.ppi() < s.ppiMin() || info.ppi() > s.ppiMax()) {
                return String.format(Locale.ROOT, "resolution %d ppi is outside the allowed %d–%d ppi",
                        info.ppi(), s.ppiMin(), s.ppiMax());
            }
        } else if (s.requirePpi()) {
            return "image does not declare its resolution (ppi)";
        }
        if (info.format() == ImageFormat.WSQ && info.compressionRatio() != null
                && info.compressionRatio() > s.wsqMaxCompressionRatio()) {
            return String.format(Locale.ROOT, "WSQ compression %.1f:1 exceeds the maximum %.0f:1",
                    info.compressionRatio(), s.wsqMaxCompressionRatio());
        }
        return null;
    }

    /** Capture rules — the operator must re-scan. */
    static String firstQualityProblem(ImageInfo info, ImageValidationSettings s) {
        if (s.checkBlank() && info.stdDev() != null && info.stdDev() < s.minStdDev()) {
            return String.format(Locale.ROOT, "image appears blank or uniform (contrast %.1f below %.0f)",
                    info.stdDev(), s.minStdDev());
        }
        if (s.checkCoverage() && info.foregroundRatio() != null && info.foregroundRatio() < s.minForegroundRatio()) {
            return String.format(Locale.ROOT,
                    "too little fingerprint area in the image (%.0f%% of blocks have ridge texture, minimum %.0f%%)",
                    info.foregroundRatio() * 100, s.minForegroundRatio() * 100);
        }
        return null;
    }

    private static String formats(ImageValidationSettings s) {
        return String.join(" or ", s.allowedFormats().stream().map(Enum::name).sorted().toList());
    }

    private static String mb(long bytes) {
        return String.format(Locale.ROOT, "%.1f MB", bytes / 1048576.0);
    }
}
