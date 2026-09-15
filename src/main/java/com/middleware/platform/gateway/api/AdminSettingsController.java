package com.middleware.platform.gateway.api;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.common.settings.PlatformSetting;
import com.middleware.platform.common.settings.PlatformSettingsService;
import com.middleware.platform.gateway.imagecheck.ImageValidationSettings;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Runtime platform settings editable by SUPER_ADMIN / PLATFORM_OPS (the
 * /admin/** catch-all in SecurityConfig). Changes take effect within seconds,
 * no redeploy.
 */
@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final PlatformSettingsService settings;

    public record SettingEnvelope<T>(T value, String updatedBy, Instant updatedAt) {}

    @GetMapping("/image-validation")
    public SettingEnvelope<ImageValidationSettings> getImageValidation() {
        ImageValidationSettings v = settings.get(ImageValidationSettings.KEY, ImageValidationSettings.class,
                ImageValidationSettings::defaults);
        PlatformSetting meta = settings.meta(ImageValidationSettings.KEY);
        return new SettingEnvelope<>(v, meta == null ? null : meta.getUpdatedBy(),
                meta == null ? null : meta.getUpdatedAt());
    }

    @PutMapping("/image-validation")
    public SettingEnvelope<ImageValidationSettings> putImageValidation(
            @Valid @RequestBody ImageValidationSettings req, Authentication auth) {
        String err = req.validationError();
        if (err != null) throw new ApplicationException(ErrorCode.VALIDATION_FAILED, err);
        ImageValidationSettings saved = settings.put(ImageValidationSettings.KEY, req, auth.getName());
        PlatformSetting meta = settings.meta(ImageValidationSettings.KEY);
        return new SettingEnvelope<>(saved, meta.getUpdatedBy(), meta.getUpdatedAt());
    }
}
