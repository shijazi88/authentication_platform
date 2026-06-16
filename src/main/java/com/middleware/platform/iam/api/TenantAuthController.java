package com.middleware.platform.iam.api;

import com.middleware.platform.iam.dto.LoginRequest;
import com.middleware.platform.iam.dto.TenantLoginResponse;
import com.middleware.platform.iam.service.TenantUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tenant-portal login. Public (permitAll in SecurityConfig). */
@RestController
@RequestMapping("/portal-api/auth")
@RequiredArgsConstructor
public class TenantAuthController {

    private final TenantUserService tenantUserService;

    @PostMapping("/login")
    public TenantLoginResponse login(@Valid @RequestBody LoginRequest req) {
        return tenantUserService.login(req);
    }
}
