package com.middleware.platform.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDeviceRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String model,
        @Size(max = 64) String type,
        @NotBlank @Size(max = 128) String serialNumber
) {}
