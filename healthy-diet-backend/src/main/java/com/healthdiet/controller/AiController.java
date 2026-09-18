package com.healthdiet.controller;

import com.healthdiet.common.ApiResponse;
import com.healthdiet.dto.AiAdviceRequest;
import com.healthdiet.service.AiNutritionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiNutritionService aiNutritionService;

    @PostMapping("/nutrition-advice")
    public ApiResponse<Map<String,String>> nutritionAdvice(@RequestBody AiAdviceRequest req){
        String answer = aiNutritionService.askNutrition(req.getQuestion(), req.getProfileSummary());
        return ApiResponse.ok(Map.of("answer", answer));
    }
}