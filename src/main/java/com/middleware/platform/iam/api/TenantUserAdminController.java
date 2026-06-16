package com.middleware.platform.iam.api;

import com.middleware.platform.iam.dto.CreateTenantUserRequest;
import com.middleware.platform.iam.dto.ResetPasswordRequest;
import com.middleware.platform.iam.dto.TenantUserResponse;
import com.middleware.platform.iam.service.TenantUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin management of a tenant's portal users. Authorization follows the
 * /admin/tenants/** matrix (reads: all roles; writes: super/ops).
 */
@RestController
@RequestMapping("/admin/tenants/{tenantId}/portal-users")
@RequiredArgsConstructor
public class TenantUserAdminController {

    private final TenantUserService tenantUserService;

    @GetMapping
    public List<TenantUserResponse> list(@PathVariable UUID tenantId) {
        return tenantUserService.listByTenant(tenantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantUserResponse create(@PathVariable UUID tenantId,
                                     @Valid @RequestBody CreateTenantUserRequest req) {
        return tenantUserService.create(tenantId, req);
    }

    @PutMapping("/{id}/status")
    public TenantUserResponse setStatus(@PathVariable UUID tenantId, @PathVariable UUID id,
                                        @RequestParam boolean active) {
        return tenantUserService.setActive(tenantId, id, active);
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID tenantId, @PathVariable UUID id,
                              @Valid @RequestBody ResetPasswordRequest req) {
        tenantUserService.resetPassword(tenantId, id, req);
    }
}
