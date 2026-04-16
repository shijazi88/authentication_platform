package com.middleware.platform.catalog.api;

import com.middleware.platform.catalog.domain.FieldDefinition;
import com.middleware.platform.catalog.domain.ServiceDefinition;
import com.middleware.platform.catalog.domain.ServiceOperation;
import com.middleware.platform.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/services")
    public List<ServiceDefinition> services() {
        return catalogService.listServices();
    }

    @GetMapping("/services/{serviceId}/operations")
    public List<ServiceOperation> operations(@PathVariable UUID serviceId) {
        return catalogService.listOperations(serviceId);
    }

    @GetMapping("/operations/{operationId}/fields")
    public List<FieldDefinition> fields(@PathVariable UUID operationId) {
        return catalogService.listFields(operationId);
    }
}
