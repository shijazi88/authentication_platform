package com.middleware.platform.iam.service;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.common.util.Ids;
import com.middleware.platform.iam.domain.ApiCredential;
import com.middleware.platform.iam.domain.Tenant;
import com.middleware.platform.iam.domain.TenantStatus;
import com.middleware.platform.iam.dto.CreateCredentialRequest;
import com.middleware.platform.iam.dto.CreateTenantRequest;
import com.middleware.platform.iam.dto.CredentialResponse;
import com.middleware.platform.iam.dto.TenantResponse;
import com.middleware.platform.iam.repo.ApiCredentialRepository;
import com.middleware.platform.iam.repo.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ApiCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TenantResponse create(CreateTenantRequest req) {
        if (tenantRepository.existsByCode(req.code())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "Tenant code already exists: " + req.code());
        }
        Tenant tenant = Tenant.builder()
                .code(req.code())
                .legalName(req.legalName())
                .contactEmail(req.contactEmail())
                .status(TenantStatus.PENDING)
                .build();
        tenantRepository.save(tenant);
        return TenantResponse.from(tenant);
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID id) {
        return tenantRepository.findById(id)
                .map(TenantResponse::from)
                .orElseThrow(() -> ApplicationException.notFound("Tenant"));
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        return tenantRepository.findAll().stream().map(TenantResponse::from).toList();
    }

    @Transactional
    public TenantResponse setStatus(UUID id, TenantStatus status) {
        Tenant t = tenantRepository.findById(id)
                .orElseThrow(() -> ApplicationException.notFound("Tenant"));
        t.setStatus(status);
        return TenantResponse.from(t);
    }

    @Transactional
    public CredentialResponse issueCredential(UUID tenantId, CreateCredentialRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApplicationException.notFound("Tenant"));

        String clientId = Ids.newClientId();
        String clientSecret = Ids.newClientSecret();
        String hash = passwordEncoder.encode(clientSecret);

        ApiCredential cred = ApiCredential.builder()
                .tenantId(tenant.getId())
                .clientId(clientId)
                .clientSecretHash(hash)
                .label(req.label())
                .ipAllowlist(req.ipAllowlist())
                .active(true)
                .build();
        credentialRepository.save(cred);

        return new CredentialResponse(
                cred.getId(),
                tenant.getId(),
                clientId,
                clientSecret,
                cred.getLabel(),
                cred.getCreatedAt()
        );
    }

    @Transactional
    public void revokeCredential(UUID credentialId) {
        ApiCredential cred = credentialRepository.findById(credentialId)
                .orElseThrow(() -> ApplicationException.notFound("Credential"));
        cred.setActive(false);
    }
}
