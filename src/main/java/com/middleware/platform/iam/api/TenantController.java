package com.middleware.platform.iam.api;

import com.middleware.platform.iam.domain.TenantStatus;
import com.middleware.platform.iam.dto.CreateCredentialRequest;
import com.middleware.platform.iam.dto.CreateTenantRequest;
import com.middleware.platform.iam.dto.CredentialResponse;
import com.middleware.platform.iam.dto.TenantResponse;
import com.middleware.platform.iam.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest req) {
        return tenantService.create(req);
    }

    @GetMapping
    public List<TenantResponse> list() {
        return tenantService.list();
    }

    @GetMapping("/{id}")
    public TenantResponse get(@PathVariable UUID id) {
        return tenantService.get(id);
    }

    @PostMapping("/{id}/status")
    public TenantResponse setStatus(@PathVariable UUID id, @RequestParam TenantStatus status) {
        return tenantService.setStatus(id, status);
    }

    /** Toggle whether this tenant must send encrypted (JWE) verification PII. */
    @PutMapping("/{id}/encryption-policy")
    public TenantResponse setEncryptionPolicy(@PathVariable UUID id, @RequestParam boolean required) {
        return tenantService.setEncryptionPolicy(id, required);
    }

    @PostMapping("/{id}/credentials")
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialResponse issueCredential(@PathVariable UUID id,
                                              @Valid @RequestBody CreateCredentialRequest req) {
        return tenantService.issueCredential(id, req);
    }

    @DeleteMapping("/credentials/{credentialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeCredential(@PathVariable UUID credentialId) {
        tenantService.revokeCredential(credentialId);
    }
}
