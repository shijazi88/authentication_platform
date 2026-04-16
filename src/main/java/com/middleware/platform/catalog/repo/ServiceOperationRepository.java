package com.middleware.platform.catalog.repo;

import com.middleware.platform.catalog.domain.ServiceOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOperationRepository extends JpaRepository<ServiceOperation, UUID> {
    Optional<ServiceOperation> findByServiceIdAndCode(UUID serviceId, String code);
    List<ServiceOperation> findByServiceId(UUID serviceId);
}
