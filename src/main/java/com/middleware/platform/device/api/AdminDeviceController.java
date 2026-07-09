package com.middleware.platform.device.api;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.device.dto.CreateDeviceRequest;
import com.middleware.platform.device.dto.DeviceResponse;
import com.middleware.platform.device.dto.ImportResult;
import com.middleware.platform.device.dto.UpdateDeviceRequest;
import com.middleware.platform.device.service.FingerprintDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Admin management of a tenant's fingerprint devices. Authorization follows the
 * /admin/tenants/** matrix (reads: all roles; writes: super/ops).
 */
@RestController
@RequestMapping("/admin/tenants/{tenantId}/devices")
@RequiredArgsConstructor
public class AdminDeviceController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final FingerprintDeviceService deviceService;

    @GetMapping
    public List<DeviceResponse> list(@PathVariable UUID tenantId) {
        return deviceService.list(tenantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse create(@PathVariable UUID tenantId,
                                 @Valid @RequestBody CreateDeviceRequest req,
                                 Authentication auth) {
        return deviceService.create(tenantId, req, auth.getName());
    }

    @PutMapping("/{id}")
    public DeviceResponse update(@PathVariable UUID tenantId, @PathVariable UUID id,
                                 @Valid @RequestBody UpdateDeviceRequest req) {
        return deviceService.update(tenantId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID tenantId, @PathVariable UUID id) {
        deviceService.softDelete(tenantId, id);
    }

    @PostMapping("/import")
    public ImportResult importDevices(@PathVariable UUID tenantId,
                                      @RequestParam("file") MultipartFile file,
                                      Authentication auth) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "No file uploaded");
        }
        try {
            return deviceService.importExcel(tenantId, file.getInputStream(), auth.getName());
        } catch (IOException e) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Could not read the uploaded file");
        }
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> template() {
        byte[] bytes = deviceService.template();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"fingerprint-devices-template.xlsx\"")
                .contentType(MediaType.parseMediaType(XLSX))
                .body(new ByteArrayResource(bytes));
    }
}
