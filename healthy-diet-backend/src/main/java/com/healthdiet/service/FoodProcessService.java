package com.healthdiet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdiet.dto.FoodProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodProcessService {

    private final DoubaoCutoutService doubaoCutoutService;
    private final AiNutritionService aiNutritionService;
    private final ObjectMapper objectMapper;

    /**
     * 完整处理流程：抠图 + 豆包视觉识别菜名与热量 + 营养估算
     */
    public FoodProcessResult process(String imageBase64) {

        // 1. 抠图
        String stickerBase64 = doubaoCutoutService.cutout(imageBase64);

        // 2. AI识别
        AiNutritionService.FoodRecognitionResult recognitionResult =
                aiNutritionService.recognizeFoodByImage(imageBase64);

        String dishName =
                recognitionResult.getDishName() != null
                        ? recognitionResult.getDishName()
                        : "未知菜品";

        Integer calories = recognitionResult.getCalories();

        String ingredient =
                recognitionResult.getIngredient() != null
                        ? recognitionResult.getIngredient()
                        : "";

        String category =
                recognitionResult.getCategory() != null
                        ? recognitionResult.getCategory()
                        : "食物";

        // 3. 营养估算
        AiNutritionService.FoodNutritionEstimate estimate =
                aiNutritionService.estimateFoodNutrition(
                        dishName,
                        calories,
                        ingredient
                );

        // 4. 返回结果
        return FoodProcessResult.builder()
                .stickerBase64(stickerBase64)
                .dishName(dishName)
                .category(category)
                .calories(calories != null ? calories : estimate.getCalories())
                .carbG(estimate.getCarbG())
                .proteinG(estimate.getProteinG())
                .fatG(estimate.getFatG())
                .fiberG(estimate.getFiberG())
                .sugarG(estimate.getSugarG())
                .sodiumMg(estimate.getSodiumMg())
                .tips(estimate.getTips())
                .ingredient(ingredient)
                .build();
    }
}