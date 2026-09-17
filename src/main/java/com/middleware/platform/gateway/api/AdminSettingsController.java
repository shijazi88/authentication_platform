package com.middleware.platform.gateway.api;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.common.settings.PlatformSetting;
import com.middleware.platform.common.settings.PlatformSettingsService;
import com.middleware.platform.common.resilience.ResilienceSettings;
import com.middleware.platform.common.resilience.ResilienceSettingsService;
import com.middleware.platform.gateway.imagecheck.ImageValidationSettings;
import com.middleware.platform.gateway.imagecheck.Nfiq2Client;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

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
    private final ResilienceSettingsService resilience;
    private final Nfiq2Client nfiq2Client;

    public record SettingEnvelope<T>(T value, String updatedBy, Instant updatedAt) {}

    @GetMapping("/image-validation")
    public SettingEnvelope<ImageValidationSettings> getImageValidation() {
        ImageValidationSettings v = settings.get(ImageValidationSettings.KEY, ImageValidationSettings.class,
                ImageValidationSettings::defaults).normalized();
        PlatformSetting meta = settings.meta(ImageValidationSettings.KEY);
        return new SettingEnvelope<>(v, meta == null ? null : meta.getUpdatedBy(),
                meta == null ? null : meta.getUpdatedAt());
    }

    @PutMapping("/image-validation")
    public SettingEnvelope<ImageValidationSettings> putImageValidation(
            @Valid @RequestBody ImageValidationSettings req, Authentication auth) {
        String err = req.validationError();
        if (err != null) throw new ApplicationException(ErrorCode.VALIDATION_FAILED, err);
        ImageValidationSettings saved = settings.put(ImageValidationSettings.KEY, req.normalized(), auth.getName());
        PlatformSetting meta = settings.meta(ImageValidationSettings.KEY);
        return new SettingEnvelope<>(saved, meta.getUpdatedBy(), meta.getUpdatedAt());
    }

    /** Live status of the NFIQ 2 quality service (sidecar) for the settings badge / dashboard alert. */
    @GetMapping("/image-validation/quality-service")
    public Nfiq2Client.Health qualityService() {
        return nfiq2Client.health();
    }

    // ── Service protection (circuit breaker + retry) ─────────────────────────

    @GetMapping("/resilience")
    public SettingEnvelope<ResilienceSettings> getResilience() {
        PlatformSetting meta = settings.meta(ResilienceSettings.KEY);
        return new SettingEnvelope<>(resilience.current(), meta == null ? null : meta.getUpdatedBy(),
                meta == null ? null : meta.getUpdatedAt());
    }

    @PutMapping("/resilience")
    public SettingEnvelope<ResilienceSettings> putResilience(@Valid @RequestBody ResilienceSettings req,
                                                             Authentication auth) {
        String err = req.validationError();
        if (err != null) throw new ApplicationException(ErrorCode.VALIDATION_FAILED, err);
        ResilienceSettings saved = resilience.save(req, auth.getName());
        PlatformSetting meta = settings.meta(ResilienceSettings.KEY);
        return new SettingEnvelope<>(saved, meta.getUpdatedBy(), meta.getUpdatedAt());
    }

    /** Live breaker state per connector. */
    @GetMapping("/resilience/status")
    public List<ResilienceSettingsService.BreakerStatus> resilienceStatus() {
        return resilience.status();
    }

    /** Close an open breaker by hand (e.g. once the provider is confirmed healthy again). */
    @PostMapping("/resilience/reset")
    public List<ResilienceSettingsService.BreakerStatus> resilienceReset(@RequestParam(required = false) String connector) {
        return resilience.reset(connector);
    }
}
