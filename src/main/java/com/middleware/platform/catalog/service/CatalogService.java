package com.middleware.platform.catalog.service;

import com.middleware.platform.catalog.domain.FieldDefinition;
import com.middleware.platform.catalog.domain.ServiceDefinition;
import com.middleware.platform.catalog.domain.ServiceOperation;
import com.middleware.platform.catalog.repo.FieldDefinitionRepository;
import com.middleware.platform.catalog.repo.ServiceDefinitionRepository;
import com.middleware.platform.catalog.repo.ServiceOperationRepository;
import com.middleware.platform.common.error.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ServiceDefinitionRepository serviceRepository;
    private final ServiceOperationRepository operationRepository;
    private final FieldDefinitionRepository fieldRepository;

    @Transactional(readOnly = true)
    public List<ServiceDefinition> listServices() {
        return serviceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ServiceDefinition getServiceByCode(String code) {
        return serviceRepository.findByCode(code)
                .orElseThrow(() -> ApplicationException.notFound("Service " + code));
    }

    @Transactional(readOnly = true)
    public ServiceOperation getOperation(String serviceCode, String operationCode) {
        ServiceDefinition svc = getServiceByCode(serviceCode);
        return operationRepository.findByServiceIdAndCode(svc.getId(), operationCode)
                .orElseThrow(() -> ApplicationException.notFound("Operation " + serviceCode + "/" + operationCode));
    }

    @Transactional(readOnly = true)
    public List<ServiceOperation> listOperations(UUID serviceId) {
        return operationRepository.findByServiceId(serviceId);
    }

    @Transactional(readOnly = true)
    public List<FieldDefinition> listFields(UUID operationId) {
        return fieldRepository.findByOperationId(operationId);
    }
}
