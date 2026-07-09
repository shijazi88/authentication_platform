package com.middleware.platform.device.repo;

import com.middleware.platform.device.domain.FingerprintDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FingerprintDeviceRepository extends JpaRepository<FingerprintDevice, UUID> {

    List<FingerprintDevice> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(UUID tenantId);

    Optional<FingerprintDevice> findByIdAndDeletedFalse(UUID id);

    boolean existsBySerialNumberIgnoreCaseAndDeletedFalse(String serialNumber);

    Optional<FingerprintDevice> findBySerialNumberIgnoreCaseAndDeletedFalse(String serialNumber);
}
