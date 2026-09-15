package com.middleware.platform.gateway.imagecheck;

import com.middleware.platform.common.settings.PlatformSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.middleware.platform.gateway.imagecheck.ImageValidationResult.Status;

/**
 * Applies the admin-configured {@link ImageValidationSettings} to a fingerprint
 * image. Rules are evaluated in order of cost and only the first failure is
 * reported, phrased so the bank's operator can act on it (re-scan, change
 * format, fix scanner resolution).
 */
@Component
@RequiredArgsConstructor
public class FingerprintImageValidator {

    private final PlatformSettingsService settings;

    public ImageValidationSettings currentSettings() {
        return settings.get(ImageValidationSettings.KEY, ImageValidationSettings.class,
                ImageValidationSettings::defaults);
    }

    /** Inspects and validates a base64 image with the current runtime settings. */
    public ImageValidationResult validate(String base64) {
        return validate(ImageInspector.inspect(base64), currentSettings());
    }

    public ImageValidationResult validate(ImageInfo info, ImageValidationSettings s) {
        if (!s.enabled()) return new ImageValidationResult(Status.SKIP, null, info);
        String problem = firstProblem(info, s);
        if (problem == null) return new ImageValidationResult(Status.PASS, null, info);
        Status st = s.mode() == ImageValidationSettings.Mode.ENFORCE ? Status.FAIL : Status.WARN;
        return new ImageValidationResult(st, problem, info);
    }

    static String firstProblem(ImageInfo info, ImageValidationSettings s) {
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
            return "PNG must be 8-bit greyscale (got "
                    + (info.bitDepth() != null ? info.bitDepth() + "-bit " : "")
                    + (Boolean.FALSE.equals(info.grayscale()) ? "colour" : "greyscale") + ")";
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
        if (s.checkBlank() && info.stdDev() != null && info.stdDev() < s.minStdDev()) {
            return String.format(Locale.ROOT, "image appears blank or uniform (contrast %.1f below %.0f)",
                    info.stdDev(), s.minStdDev());
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
