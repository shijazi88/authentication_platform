package com.middleware.platform.catalog.repo;

import com.middleware.platform.catalog.domain.ServiceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceDefinitionRepository extends JpaRepository<ServiceDefinition, UUID> {
    Optional<ServiceDefinition> findByCode(String code);
}
