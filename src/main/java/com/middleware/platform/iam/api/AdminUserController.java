package com.middleware.platform.iam.api;

import com.middleware.platform.iam.dto.AdminUserResponse;
import com.middleware.platform.iam.dto.CreateAdminUserRequest;
import com.middleware.platform.iam.dto.ResetPasswordRequest;
import com.middleware.platform.iam.dto.UpdateAdminUserRequest;
import com.middleware.platform.iam.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin user management. Restricted to SUPER_ADMIN — managing accounts and
 * roles is a privileged operation, so the whole controller is gated.
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public List<AdminUserResponse> list() {
        return adminUserService.list();
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable UUID id) {
        return adminUserService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse create(@Valid @RequestBody CreateAdminUserRequest req) {
        return adminUserService.create(req);
    }

    @PutMapping("/{id}")
    public AdminUserResponse update(@PathVariable UUID id,
                                    @Valid @RequestBody UpdateAdminUserRequest req) {
        return adminUserService.update(id, req);
    }

    @PutMapping("/{id}/status")
    public AdminUserResponse setStatus(@PathVariable UUID id,
                                       @RequestParam boolean active,
                                       Authentication authentication) {
        return adminUserService.setActive(id, active, authentication.getName());
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID id,
                              @Valid @RequestBody ResetPasswordRequest req) {
        adminUserService.resetPassword(id, req);
    }
}
