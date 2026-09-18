package com.healthdiet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "healthdiet.baidu")
public class BaiduProperties {
    private String apiKey = "";
    private String secretKey = "";
}
