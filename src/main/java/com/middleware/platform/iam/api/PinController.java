package com.middleware.platform.iam.api;

import com.middleware.platform.iam.dto.PinStatusResponse;
import com.middleware.platform.iam.dto.SetPinRequest;
import com.middleware.platform.iam.dto.UnlockResponse;
import com.middleware.platform.iam.dto.VerifyPinRequest;
import com.middleware.platform.iam.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Per-user step-up PIN. Any authenticated admin manages their own PIN and
 * exchanges it for a short-lived unlock token used to open the Transactions
 * page. Authorized for all roles in SecurityConfig (/admin/auth/pin/**).
 */
@RestController
@RequestMapping("/admin/auth/pin")
@RequiredArgsConstructor
public class PinController {

    private final AdminUserService adminUserService;

    @GetMapping("/status")
    public PinStatusResponse status(Authentication authentication) {
        return adminUserService.pinStatus(authentication.getName());
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void set(@Valid @RequestBody SetPinRequest req, Authentication authentication) {
        adminUserService.setPin(authentication.getName(), req);
    }

    @PostMapping("/verify")
    public UnlockResponse verify(@Valid @RequestBody VerifyPinRequest req, Authentication authentication) {
        return adminUserService.verifyPin(authentication.getName(), req);
    }
}
