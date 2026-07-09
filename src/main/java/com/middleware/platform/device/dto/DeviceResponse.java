package com.middleware.platform.device.dto;

import com.middleware.platform.device.domain.FingerprintDevice;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        UUID tenantId,
        String name,
        String model,
        String type,
        String serialNumber,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static DeviceResponse from(FingerprintDevice d) {
        return new DeviceResponse(
                d.getId(), d.getTenantId(), d.getName(), d.getModel(), d.getType(),
                d.getSerialNumber(), d.getCreatedBy(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
