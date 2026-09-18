package com.healthdiet.controller;

import com.healthdiet.common.ApiResponse;
import com.healthdiet.entity.Plan;
import com.healthdiet.repository.UserRepository;
import com.healthdiet.service.PlanService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final UserRepository userRepository;

    /**
     * 获取本周饮食计划
     */
    @GetMapping("/week")
    public ApiResponse<List<Plan>> getWeekPlan(
            @RequestHeader("token") String token,
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        var user = userRepository.findByToken(token.replace("Bearer ", ""))
                .orElseThrow(() -> new RuntimeException("未登录"));

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<Plan> plans = planService.getOrCreateWeekPlan(
                user.getId(),
                start,
                end
        );

        return ApiResponse.ok(plans);
    }

    /**
     * 修改某一天某一餐
     */
    @PostMapping("/update")
    public ApiResponse<Void> updateMeal(
            @RequestHeader("token") String token,
            @RequestBody UpdateMealRequest req
    ) {
        var user = userRepository.findByToken(token.replace("Bearer ", ""))
                .orElseThrow(() -> new RuntimeException("未登录"));

        LocalDate date = LocalDate.parse(req.getDate());

        planService.updateMeal(
                user.getId(),
                date,
                req.getMealType(),
                req.getRecipeIds()
        );

        return ApiResponse.ok();
    }

    /**
     * 请求参数
     */
    @Data
    public static class UpdateMealRequest {

        /**
         * 1早餐 2午餐 3晚餐
         */
        private Integer mealType;

        /**
         * 菜谱ID列表
         */
        private List<Integer> recipeIds;

        /**
         * 日期
         * 格式：2026-06-08
         */
        private String date;
    }
}