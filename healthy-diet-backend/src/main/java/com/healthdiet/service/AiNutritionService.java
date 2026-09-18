package com.healthdiet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthdiet.common.BusinessException;
import com.healthdiet.config.DoubaoProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiNutritionService {

    private final DoubaoProperties doubaoProperties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 使用视觉豆包模型识别菜品名称、热量和主要食材
     */
    public FoodRecognitionResult recognizeFoodByImage(String imageBase64) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", "doubao-1-5-vision-pro-32k-250115");

            ArrayNode messages = body.putArray("messages");
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            ArrayNode content = user.putArray("content");

            ObjectNode image = content.addObject();
            image.put("type", "image_url");
            ObjectNode imageUrl = image.putObject("image_url");
            imageUrl.put("url", "data:image/jpeg;base64," + imageBase64);

            ObjectNode text = content.addObject();
            text.put("type", "text");
            text.put("text", """
                    请识别图片中的内容。

                    category只能返回：
                    食物
                    饮料

                    严格返回JSON：

                    {
                      "dishName":"菜名",
                      "category":"食物",
                      "calories":300,
                      "ingredient":"主要食材"
                    }

                    示例：

                    奶茶 -> 饮料
                    可乐 -> 饮料
                    咖啡 -> 饮料
                    果汁 -> 饮料
                    豆浆 -> 饮料
                    果粒橙 -> 饮料
                    橙汁 -> 饮料
                    牛奶 -> 饮料
                    红茶 -> 饮料
                    绿茶 -> 饮料

                    米饭 -> 食物
                    汉堡 -> 食物
                    炒饭 -> 食物
                    面条 -> 食物

                    不要返回markdown。
                    不要返回解释。
                    只返回JSON。
                    """);

            String json = objectMapper.writeValueAsString(body);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(doubaoProperties.getEndpoint()))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + doubaoProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp =
                    httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            JsonNode root = objectMapper.readTree(resp.body());

            if (root.has("error")) {
                throw BusinessException.of(
                        "豆包视觉识别错误: "
                                + root.path("error").path("message").asText(resp.body()));
            }

            JsonNode contentNode =
                    root.path("choices")
                            .get(0)
                            .path("message")
                            .path("content");

            JsonNode jsonNode =
                    objectMapper.readTree(contentNode.asText());

            String dishName =
                    jsonNode.path("dishName").asText("未知菜品");

            Integer calories =
                    jsonNode.has("calories")
                            ? jsonNode.get("calories").asInt()
                            : null;

            String ingredient =
                    jsonNode.path("ingredient").asText("");

            String category =
                    jsonNode.path("category").asText("食物");

            return new FoodRecognitionResult(
                    dishName,
                    calories,
                    ingredient,
                    category
            );

        } catch (Exception e) {
            log.error("豆包视觉识别失败", e);
            throw BusinessException.of("豆包视觉识别失败: " + e.getMessage());
        }
    }

    /**
     * 根据菜名估算营养数据与 tips（JSON 格式返回）
     */
    public FoodNutritionEstimate estimateFoodNutrition(String dishName, Integer knownCalories, String ingredient) {
        String prompt = "请为中餐菜品「" + dishName + "」估算一份（约200-300g）的营养数据。"
                + (knownCalories != null ? "参考热量约" + knownCalories + "kcal。" : "")
                + (ingredient != null && !ingredient.isBlank() ? "主要食材：" + ingredient + "。" : "")
                + "严格只返回 JSON，不要 markdown，格式："
                + "{\"calories\":285,\"carbG\":12.5,\"proteinG\":14.2,\"fatG\":20.4,"
                + "\"fiberG\":1.8,\"sugarG\":8.5,\"sodiumMg\":650,"
                + "\"tips\":\"请结合当前食品特点生成80字以内营养建议，"
                + "不要使用通用模板，"
                + "饮料重点分析糖分和添加剂，"
                + "水果重点分析维生素和糖分，"
                + "油炸食品重点分析脂肪和钠含量，"
                + "高蛋白食品重点分析蛋白质价值，"
                + "语气自然友好，像营养师给用户的个性化建议\"}";

        String raw = askNutrition(prompt, null);
        return parseNutritionEstimate(raw, dishName, knownCalories);
    }

    private FoodNutritionEstimate parseNutritionEstimate(String raw, String dishName, Integer knownCalories) {
        try {
            String json = raw.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JsonNode node = objectMapper.readTree(json);
            return FoodNutritionEstimate.builder()
                    .calories(node.has("calories") ? node.get("calories").asInt() : (knownCalories != null ? knownCalories : 300))
                    .carbG(decimal(node, "carbG", "12.5"))
                    .proteinG(decimal(node, "proteinG", "14.2"))
                    .fatG(decimal(node, "fatG", "20.4"))
                    .fiberG(decimal(node, "fiberG", "1.8"))
                    .sugarG(decimal(node, "sugarG", "8.5"))
                    .sodiumMg(decimal(node, "sodiumMg", "650"))
                    .tips(node.path("tips").asText("建议适量食用，注意均衡搭配。"))
                    .build();
        } catch (Exception e) {
            log.warn("parse nutrition json failed: {}", e.getMessage());
            return FoodNutritionEstimate.builder()
                    .calories(knownCalories != null ? knownCalories : 300)
                    .carbG(new BigDecimal("12.5"))
                    .proteinG(new BigDecimal("14.2"))
                    .fatG(new BigDecimal("20.4"))
                    .fiberG(new BigDecimal("1.8"))
                    .sugarG(new BigDecimal("8.5"))
                    .sodiumMg(new BigDecimal("650"))
                    .tips("「" + dishName + "」建议控制油盐糖用量，搭配蔬菜更均衡。")
                    .build();
        }
    }

    private static BigDecimal decimal(JsonNode node, String field, String fallback) {
        if (node.has(field)) {
            return BigDecimal.valueOf(node.get(field).asDouble());
        }
        return new BigDecimal(fallback);
    }

    @Data
    @Builder
    public static class FoodNutritionEstimate {
        private Integer calories;
        private BigDecimal carbG;
        private BigDecimal proteinG;
        private BigDecimal fatG;
        private BigDecimal fiberG;
        private BigDecimal sugarG;
        private BigDecimal sodiumMg;
        private String tips;
    }

    @Data
    @Builder
    public static class FoodRecognitionResult {

        private final String dishName;

        private final Integer calories;

        private final String ingredient;

        private final String category;
    }

    public String askNutrition(String userQuestion, String profileSummary) {
        if (doubaoProperties.getApiKey() == null || doubaoProperties.getApiKey().isBlank()) {
            throw BusinessException.of("未配置豆包 API Key");
        }
        if (doubaoProperties.getModel() == null || doubaoProperties.getModel().isBlank()) {
            throw BusinessException.of("未配置豆包推理接入点 model");
        }
        String system = "你是一位专业营养师与中餐膳食顾问，回答简洁、可执行，注意用户过敏与忌口。若信息不足请明确说明假设。";
        if (profileSummary != null && !profileSummary.isBlank()) {
            system += "\n用户档案摘要：\n" + profileSummary;
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", doubaoProperties.getModel());
        ArrayNode messages = body.putArray("messages");
        ObjectNode m0 = messages.addObject();
        m0.put("role", "system");
        m0.put("content", system);
        ObjectNode m1 = messages.addObject();
        m1.put("role", "user");
        m1.put("content", userQuestion);

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(doubaoProperties.getEndpoint()))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + doubaoProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(resp.body());
            if (root.has("error")) {
                throw BusinessException.of("大模型错误: " + root.path("error").path("message").asText(resp.body()));
            }
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return resp.body();
            }
            return choices.get(0).path("message").path("content").asText("（无正文）");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("doubao", e);
            throw BusinessException.of("调用大模型失败: " + e.getMessage());
        }
    }
}