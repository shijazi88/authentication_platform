package com.middleware.platform.catalog.repo;

import com.middleware.platform.catalog.domain.FieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FieldDefinitionRepository extends JpaRepository<FieldDefinition, UUID> {
    List<FieldDefinition> findByOperationId(UUID operationId);
}
