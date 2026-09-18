package com.healthdiet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdiet.common.BusinessException;
import com.healthdiet.config.BaiduProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * 百度「菜品识别」REST 调用。需在 application.yml 或环境变量配置 API Key / Secret Key。
 * 参考文档：https://ai.baidu.com/ai-doc/IMAGERECOGNITION/pk3bcxe71
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaiduDishRecognitionService {

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String DISH_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v2/dish";

    private final BaiduProperties baiduProperties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile String accessToken;
    private volatile Instant tokenExpireAt = Instant.EPOCH;

    public JsonNode recognizeDish(String imageBase64) {
        if (baiduProperties.getApiKey() == null || baiduProperties.getApiKey().isBlank()) {
            throw BusinessException.of("未配置百度 API Key（healthdiet.baidu.api-key 或 BAIDU_API_KEY）");
        }
        if (baiduProperties.getSecretKey() == null || baiduProperties.getSecretKey().isBlank()) {
            throw BusinessException.of("未配置百度 Secret Key");
        }
        String b64 = stripDataUrl(imageBase64);
        String token = getAccessToken();
        String form = "image=" + URLEncoder.encode(b64, StandardCharsets.UTF_8) + "&top_num=5&filter_threshold=0.7";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(DISH_URL + "?access_token=" + token))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(resp.body());
            if (root.has("error_code") && root.get("error_code").asInt() != 0) {
                throw BusinessException.of("百度识别失败: " + root.path("error_msg").asText(resp.body()));
            }
            return root;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("baidu dish", e);
            throw BusinessException.of("调用百度菜品识别异常: " + e.getMessage());
        }
    }

    /** 供抠图等复用百度 Token 的服务调用 */
    public String getAccessTokenForExternal() {
        return getAccessToken();
    }

    private String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(tokenExpireAt)) {
            return accessToken;
        }
        synchronized (this) {
            if (accessToken != null && Instant.now().isBefore(tokenExpireAt)) {
                return accessToken;
            }
            String url = TOKEN_URL + "?grant_type=client_credentials&client_id="
                    + URLEncoder.encode(baiduProperties.getApiKey(), StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(baiduProperties.getSecretKey(), StandardCharsets.UTF_8);
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .timeout(Duration.ofSeconds(15))
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                JsonNode root = objectMapper.readTree(resp.body());
                if (!root.has("access_token")) {
                    throw BusinessException.of("获取百度 access_token 失败: " + resp.body());
                }
                accessToken = root.get("access_token").asText();
                int expiresIn = root.path("expires_in").asInt(2592000);
                tokenExpireAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 300));
                return accessToken;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("baidu token", e);
                throw BusinessException.of("获取百度 Token 失败: " + e.getMessage());
            }
        }
    }

    private static String stripDataUrl(String imageBase64) {
        String s = imageBase64.trim();
        int idx = s.indexOf("base64,");
        if (idx > 0) {
            return s.substring(idx + "base64,".length());
        }
        return s;
    }
    public String recognizeDishName(String imageBase64) {
        JsonNode root = recognizeDish(imageBase64);

        JsonNode result = root.path("result");

        if (result.isArray() && result.size() > 0) {
            return result.get(0).path("name").asText();
        }

        return "未知菜品";
    }
}
