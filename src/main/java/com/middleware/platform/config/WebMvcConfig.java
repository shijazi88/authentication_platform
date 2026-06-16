package com.middleware.platform.config;

import com.middleware.platform.iam.security.TxnUnlockInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the PIN step-up gate on the Transactions endpoints. Read access to
 * transaction data requires a valid unlock token in addition to a normal login.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TxnUnlockInterceptor txnUnlockInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(txnUnlockInterceptor)
                .addPathPatterns("/admin/transactions/**");
    }
}
