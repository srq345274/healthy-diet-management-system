package com.healthdiet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthdiet.common.BusinessException;
import com.healthdiet.config.DoubaoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 豆包 SeedEdit 抠图，失败时回退百度智能抠图。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoubaoCutoutService {

    private static final String IMAGES_URL = "https://ark.cn-beijing.volces.com/api/v3/images/generations";
    private static final String BAIDU_SEGMENT_URL = "https://aip.baidubce.com/rest/2.0/image-process/v1/segment";

    private final DoubaoProperties doubaoProperties;
    private final BaiduDishRecognitionService baiduDishRecognitionService;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * 抠图：豆包 → 百度 → 原图（保证识别流程不中断）
     */
    public String cutout(String imageBase64) {
        try {
            return cutoutByDoubao(imageBase64);
        } catch (Exception e) {
            log.warn("豆包抠图失败，回退百度抠图: {}", e.getMessage());
        }
        try {
            return cutoutByBaidu(imageBase64);
        } catch (Exception e) {
            log.warn("百度抠图失败，使用裁剪原图: {}", e.getMessage());
            return stripDataUrl(imageBase64);
        }
    }

    private String cutoutByDoubao(String imageBase64) {
        if (doubaoProperties.getApiKey() == null || doubaoProperties.getApiKey().isBlank()) {
            throw BusinessException.of("未配置豆包 API Key");
        }
        String model = doubaoProperties.getCutoutModel();
        if (model == null || model.isBlank()) {
            model = "doubao-seededit-3-0-i2i-250628";
        }
        String b64 = stripDataUrl(imageBase64);
        String dataUrl = "data:image/jpeg;base64," + b64;

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("prompt", "去除背景，只保留食物主体，背景改为透明，不要添加文字或装饰");
        body.put("image", dataUrl);
        body.put("response_format", "b64_json");
        body.put("watermark", false);

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(IMAGES_URL))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + doubaoProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(resp.body());
            if (root.has("error")) {
                throw BusinessException.of("豆包抠图错误: " + root.path("error").path("message").asText(resp.body()));
            }
            JsonNode data = root.path("data");
            if (data.isArray() && !data.isEmpty()) {
                JsonNode first = data.get(0);
                if (first.has("b64_json") && !first.get("b64_json").asText("").isBlank()) {
                    return first.get("b64_json").asText();
                }
                if (first.has("url") && !first.get("url").asText("").isBlank()) {
                    return downloadAsBase64(first.get("url").asText());
                }
            }
            throw BusinessException.of("豆包抠图未返回有效图片");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("doubao cutout", e);
            throw BusinessException.of("豆包抠图异常: " + e.getMessage());
        }
    }

    private String cutoutByBaidu(String imageBase64) {
        String b64 = stripDataUrl(imageBase64);
        String token = baiduDishRecognitionService.getAccessTokenForExternal();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("image", b64);
        body.put("return_form", "rgba");
        body.put("method", "auto");
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BAIDU_SEGMENT_URL + "?access_token=" + token))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(resp.body());
            if (root.has("error_code") && root.get("error_code").asInt() != 0) {
                throw BusinessException.of("百度抠图失败: " + root.path("error_msg").asText(resp.body()));
            }
            String result = root.path("image").asText("");
            if (result.isBlank()) {
                throw BusinessException.of("百度抠图未返回图片");
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("baidu cutout", e);
            throw BusinessException.of("百度抠图异常: " + e.getMessage());
        }
    }

    private String downloadAsBase64(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        return java.util.Base64.getEncoder().encodeToString(resp.body());
    }

    private static String stripDataUrl(String imageBase64) {
        String s = imageBase64.trim();
        int idx = s.indexOf("base64,");
        if (idx > 0) {
            return s.substring(idx + "base64,".length());
        }
        return s;
    }
}
