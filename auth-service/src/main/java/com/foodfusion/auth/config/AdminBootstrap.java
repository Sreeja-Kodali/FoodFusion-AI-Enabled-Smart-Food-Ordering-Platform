package com.foodfusion.auth.config;

import com.foodfusion.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class AdminBootstrap {
    @Bean
    ApplicationRunner bootstrapAdmin(
            AuthService authService,
            @Value("${FOODFUSION_ADMIN_EMAIL:}") String email,
            @Value("${FOODFUSION_ADMIN_PASSWORD:}") String password
    ) {
        return args -> {
            if (StringUtils.hasText(email) && StringUtils.hasText(password)) {
                if (password.length() < 12) {
                    throw new IllegalStateException("FOODFUSION_ADMIN_PASSWORD must contain at least 12 characters.");
                }
                authService.createBootstrapAdmin(email, password);
            }
        };
    }
}
