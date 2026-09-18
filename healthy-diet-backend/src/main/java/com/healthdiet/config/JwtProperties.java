package com.healthdiet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "healthdiet.jwt")
public class JwtProperties {
    private String secret = "change-me";
    private long expirationMs = 86400000L;
}
