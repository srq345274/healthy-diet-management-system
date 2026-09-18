package com.healthdiet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "healthdiet.doubao")
public class DoubaoProperties {
    private String apiKey = "";
    private String endpoint = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    private String model = "";
    /** 豆包抠图模型（SeedEdit 图生图） */
    private String cutoutModel = "doubao-seededit-3-0-i2i-250628";
}
