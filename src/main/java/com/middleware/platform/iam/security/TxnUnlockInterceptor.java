package com.middleware.platform.iam.security;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces PIN step-up on sensitive endpoints (Transactions). The caller must
 * present a valid, unexpired unlock token — issued by PinController#verify — in
 * the {@value #HEADER} header, bound to the authenticated user. Missing/invalid
 * ⇒ 423 LOCKED so the portal knows to prompt for the PIN.
 */
@Component
@RequiredArgsConstructor
public class TxnUnlockInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Txn-Unlock";

    private final JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? auth.getName() : null;
        String token = request.getHeader(HEADER);
        if (!jwtService.isValidUnlock(token, email)) {
            throw new ApplicationException(ErrorCode.PIN_UNLOCK_REQUIRED, "PIN unlock required");
        }
        return true;
    }
}
