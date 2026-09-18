package com.healthdiet.config;

import com.healthdiet.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WebConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {

        FilterRegistrationBean<JwtAuthenticationFilter> reg =
                new FilterRegistrationBean<>();

        reg.setFilter(jwtAuthenticationFilter);

        reg.addUrlPatterns("/api/*");

        // 放行无需登录接口
        reg.addInitParameter(
                "exclusions",
                String.join(",",
                        "/api/health",
                        "/api/email/send-code",
                        "/api/email/check-email-exists",
                        "/api/auth/email-register",
                        "/api/auth/email-pwd-login",
                        "/api/auth/email-code-login",
                        "/api/auth/email-reset-pwd"
                )
        );

        reg.setOrder(1);

        return reg;
    }
}
