package com.healthdiet.controller;

import com.healthdiet.entity.User;
import com.healthdiet.repository.UserRepository;
import com.healthdiet.service.FoodListService;
import com.healthdiet.service.HealthDataService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/food-list")
@RequiredArgsConstructor
public class FoodListController {

    private final FoodListService foodListService;
    private final HealthDataService healthDataService;
    private final UserRepository userRepository;

    @Data
    public static class MealTypeRequest {
        private String mealType;
    }

    /** 获取指定日期餐次和总热量 */
    @GetMapping("/date")
    public ResponseEntity<?> getListByDate(
            @RequestHeader("token") String token,
            @RequestParam String date) {

        try {
            User user = userRepository.findByToken(token)
                    .orElseThrow(() -> new RuntimeException("未登录"));

            LocalDate queryDate = LocalDate.parse(date);

            foodListService.initDayList(user.getId(), queryDate);

            Map<String, Object> result =
                    foodListService.getListByDate(user.getId(), queryDate);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取餐次列表失败：" + e.getMessage());
        }
    }

    /** 新增餐次 */
    @PostMapping("/add")
    public ResponseEntity<?> addMeal(
            @RequestHeader("token") String token,
            @RequestBody MealTypeRequest req) {

        try {

            if (req == null
                    || req.getMealType() == null
                    || req.getMealType().trim().isEmpty()) {

                return ResponseEntity.badRequest()
                        .body("mealType不能为空");
            }

            User user = userRepository.findByToken(token)
                    .orElseThrow(() -> new RuntimeException("未登录"));

            return ResponseEntity.ok(
                    foodListService.addMeal(
                            user.getId(),
                            req.getMealType().trim(),
                            LocalDate.now()
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("创建餐次失败：" + e.getMessage());
        }
    }

    /** 获取餐次详情（包含食物图片） */
    @GetMapping("/detail")
    public ResponseEntity<?> getDetail(
            @RequestHeader("token") String token,
            @RequestParam Integer id,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        try {
            User user = userRepository.findByToken(token)
                    .orElseThrow(() -> new RuntimeException("未登录"));

            Map<String, Object> result = foodListService.getDetail(user.getId(), id, date);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取详情失败：" + e.getMessage());
        }
    }

    /** 获取最近7天体重变化和热量数据 */
    @GetMapping("/week-calorie")
    public ResponseEntity<?> getWeekCalorie(
            @RequestHeader("token") String token) {

        try {

            User user = userRepository.findByToken(token)
                    .orElseThrow(() -> new RuntimeException("未登录"));

            LocalDate today = LocalDate.now();

            // 热量只取最近7天
            LocalDate weekStart = today.minusDays(6);

            // 体重趋势取全部历史
            List<HealthDataService.WeightRecord> weights =
                    healthDataService.getWeightHistory(
                            user.getId(),
                            LocalDate.of(2000, 1, 1),
                            today
                    );

            Map<String, Integer> caloriesByDay =
                    foodListService.getCaloriesByDateRange(
                            user.getId(),
                            weekStart,
                            today
                    );

            Map<String, Object> result = new HashMap<>();
            result.put("weights", weights);
            result.put("caloriesByDay", caloriesByDay);

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取体重和热量数据失败：" + e.getMessage());
        }
    }
}