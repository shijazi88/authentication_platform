package com.middleware.platform.device.service;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.device.domain.FingerprintDevice;
import com.middleware.platform.device.dto.CreateDeviceRequest;
import com.middleware.platform.device.dto.DeviceResponse;
import com.middleware.platform.device.dto.ImportResult;
import com.middleware.platform.device.dto.UpdateDeviceRequest;
import com.middleware.platform.device.repo.FingerprintDeviceRepository;
import com.middleware.platform.iam.repo.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Per-tenant fingerprint device registry: CRUD, soft delete, unique serials,
 * and Excel import / template.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FingerprintDeviceService {

    private static final int MAX_IMPORT_ROWS = 5000;
    private static final String[] COLUMNS = {"name", "model", "type", "serialNumber"};

    private final FingerprintDeviceRepository devices;
    private final TenantRepository tenants;

    @Transactional(readOnly = true)
    public List<DeviceResponse> list(UUID tenantId) {
        return devices.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId)
                .stream().map(DeviceResponse::from).toList();
    }

    @Transactional
    public DeviceResponse create(UUID tenantId, CreateDeviceRequest req, String createdBy) {
        requireTenant(tenantId);
        String serial = req.serialNumber().trim();
        if (devices.existsBySerialNumberIgnoreCaseAndDeletedFalse(serial)) {
            throw new ApplicationException(ErrorCode.CONFLICT,
                    "A device with serial number '" + serial + "' already exists");
        }
        FingerprintDevice d = FingerprintDevice.builder()
                .tenantId(tenantId)
                .name(req.name().trim())
                .model(blankToNull(req.model()))
                .type(blankToNull(req.type()))
                .serialNumber(serial)
                .deleted(false)
                .createdBy(createdBy)
                .build();
        devices.save(d);
        return DeviceResponse.from(d);
    }

    @Transactional
    public DeviceResponse update(UUID tenantId, UUID id, UpdateDeviceRequest req) {
        FingerprintDevice d = require(tenantId, id);
        String serial = req.serialNumber().trim();
        if (!serial.equalsIgnoreCase(d.getSerialNumber())
                && devices.existsBySerialNumberIgnoreCaseAndDeletedFalse(serial)) {
            throw new ApplicationException(ErrorCode.CONFLICT,
                    "A device with serial number '" + serial + "' already exists");
        }
        d.setName(req.name().trim());
        d.setModel(blankToNull(req.model()));
        d.setType(blankToNull(req.type()));
        d.setSerialNumber(serial);
        return DeviceResponse.from(d);
    }

    @Transactional
    public void softDelete(UUID tenantId, UUID id) {
        FingerprintDevice d = require(tenantId, id);
        d.setDeleted(true);
        d.setDeletedAt(Instant.now());
    }

    /** Import devices from an uploaded .xlsx (columns: name, model, type, serialNumber). */
    @Transactional
    public ImportResult importExcel(UUID tenantId, InputStream in, String createdBy) {
        requireTenant(tenantId);
        int created = 0, skipped = 0;
        List<ImportResult.RowError> errors = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();

        try (ReadableWorkbook wb = new ReadableWorkbook(in)) {
            Sheet sheet = wb.getFirstSheet();
            List<Row> rows = sheet.read();
            for (Row row : rows) {
                int n = row.getRowNum(); // 1-based; row 1 is the header
                if (n <= 1) continue;
                if (n - 1 > MAX_IMPORT_ROWS) {
                    errors.add(new ImportResult.RowError(n, "Row limit (" + MAX_IMPORT_ROWS + ") exceeded"));
                    break;
                }
                String name = cell(row, 0), model = cell(row, 1), type = cell(row, 2), serial = cell(row, 3);
                if (name.isEmpty() && model.isEmpty() && type.isEmpty() && serial.isEmpty()) {
                    continue; // blank row
                }
                if (name.isEmpty() || serial.isEmpty()) {
                    errors.add(new ImportResult.RowError(n, "name and serialNumber are required"));
                    skipped++;
                    continue;
                }
                String key = serial.toLowerCase();
                if (!seenInFile.add(key)) {
                    errors.add(new ImportResult.RowError(n, "Duplicate serial '" + serial + "' within the file"));
                    skipped++;
                    continue;
                }
                if (devices.existsBySerialNumberIgnoreCaseAndDeletedFalse(serial)) {
                    errors.add(new ImportResult.RowError(n, "Serial '" + serial + "' already registered"));
                    skipped++;
                    continue;
                }
                devices.save(FingerprintDevice.builder()
                        .tenantId(tenantId)
                        .name(name)
                        .model(blankToNull(model))
                        .type(blankToNull(type))
                        .serialNumber(serial)
                        .deleted(false)
                        .createdBy(createdBy)
                        .build());
                created++;
            }
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Device import failed: {}", e.getMessage());
            throw new ApplicationException(ErrorCode.BAD_REQUEST,
                    "Could not read the Excel file. Use the provided template (.xlsx).");
        }
        log.info("Device import for tenant {}: created={} skipped={}", tenantId, created, skipped);
        return new ImportResult(created, skipped, errors);
    }

    /** A blank .xlsx template with the expected header row and one example. */
    public byte[] template() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (Workbook wb = new Workbook(os, "MOTABIQ", "1.0")) {
            Worksheet ws = wb.newWorksheet("Devices");
            for (int c = 0; c < COLUMNS.length; c++) {
                ws.value(0, c, COLUMNS[c]);
                ws.style(0, c).bold().set();
            }
            ws.value(1, 0, "Reception scanner");
            ws.value(1, 1, "Hamster Pro 20");
            ws.value(1, 2, "optical");
            ws.value(1, 3, "SG-EXAMPLE-0001");
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR, "Failed to build template", e);
        }
        return os.toByteArray();
    }

    private FingerprintDevice require(UUID tenantId, UUID id) {
        FingerprintDevice d = devices.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ApplicationException.notFound("Device"));
        if (!d.getTenantId().equals(tenantId)) {
            throw ApplicationException.notFound("Device");
        }
        return d;
    }

    private void requireTenant(UUID tenantId) {
        if (!tenants.existsById(tenantId)) {
            throw ApplicationException.notFound("Tenant");
        }
    }

    private static String cell(Row row, int i) {
        try {
            String v = row.getCellText(i);
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
