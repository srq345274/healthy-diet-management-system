package com.healthdiet.service;

import com.healthdiet.entity.DietRecord;
import com.healthdiet.entity.Food;
import com.healthdiet.entity.FoodList;
import com.healthdiet.repository.DietRecordRepository;
import com.healthdiet.repository.FoodListRepository;
import com.healthdiet.repository.FoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FoodListService {

    private final FoodListRepository foodListRepository;
    private final DietRecordRepository dietRecordRepository;
    private final FoodRepository foodRepository;

    @Transactional
    public void initDayList(Integer userId, LocalDate date) {
        List<String> meals = Arrays.asList("早餐", "午餐", "晚餐");
        for (String meal : meals) {
            FoodList exist = foodListRepository.findByUserIdAndDateAndMealType(userId, date, meal);
            if (exist == null) {
                FoodList foodList = new FoodList();
                foodList.setUserId(userId);
                foodList.setDate(date);
                foodList.setMealType(meal);
                foodList.setTargetKcal(500);
                foodList.setCurrentKcal(0);
                foodListRepository.save(foodList);
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getListByDate(Integer userId, LocalDate date) {
        initDayList(userId, date);
        List<FoodList> list = foodListRepository.findByUserIdAndDateOrderByIdAsc(userId, date);

        int totalKcal = 0;
        List<Map<String, Object>> listResult = new ArrayList<>();

        for (FoodList foodList : list) {
            List<DietRecord> records = dietRecordRepository.findByListIdOrderByIdAsc(foodList.getId());
            int kcal = 0;
            List<Map<String, Object>> foods = new ArrayList<>();
            for (DietRecord record : records) {
                Food food = foodRepository.findById(record.getFoodId()).orElse(null);
                if (food == null) continue;
                kcal += food.getCalories();
                Map<String, Object> item = new HashMap<>();
                item.put("id", food.getId());
                item.put("category", food.getCategory());
                item.put("name", food.getName());
                item.put("image", food.getImage());
                item.put("calories", food.getCalories());
                foods.add(item);
            }
            foodList.setCurrentKcal(kcal);
            totalKcal += kcal;
            Map<String, Object> foodListMap = new HashMap<>();
            foodListMap.put("id", foodList.getId());
            foodListMap.put("mealType", foodList.getMealType());
            foodListMap.put("targetKcal", foodList.getTargetKcal());
            foodListMap.put("currentKcal", kcal);
            foodListMap.put("foods", foods);
            listResult.add(foodListMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("totalKcal", totalKcal);
        result.put("list", listResult);
        return result;
    }

    @Transactional
    public FoodList addMeal(Integer userId, String mealType, LocalDate date) {
        FoodList exist = foodListRepository.findByUserIdAndDateAndMealType(userId, date, mealType);
        if (exist != null) throw new RuntimeException("该餐次已存在");

        FoodList foodList = new FoodList();
        foodList.setUserId(userId);
        foodList.setDate(date);
        foodList.setMealType(mealType);
        foodList.setTargetKcal(500);
        foodList.setCurrentKcal(0);
        return foodListRepository.save(foodList);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDetail(Integer userId, Integer listId, LocalDate date) {
        FoodList foodList = foodListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new RuntimeException("清单不存在"));
        if (date != null && !foodList.getDate().equals(date))
            throw new RuntimeException("指定日期的清单不存在");

        List<DietRecord> records = dietRecordRepository.findByListIdOrderByIdAsc(foodList.getId());
        List<Map<String, Object>> foods = new ArrayList<>();
        for (DietRecord record : records) {
            Food food = foodRepository.findById(record.getFoodId()).orElse(null);
            if (food == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("id", food.getId());
            item.put("category", food.getCategory());
            item.put("name", food.getName());
            item.put("image", food.getImage());
            item.put("calories", food.getCalories());
            foods.add(item);
        }

        int currentKcal = foods.stream().mapToInt(f -> (Integer) f.get("calories")).sum();
        Map<String, Object> result = new HashMap<>();
        result.put("id", foodList.getId());
        result.put("mealType", foodList.getMealType());
        result.put("targetKcal", foodList.getTargetKcal());
        result.put("currentKcal", currentKcal);
        result.put("foods", foods);
        return result;
    }

    /**
     * 获取最近几天每日总热量（不再嵌套事务）
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> getCaloriesByDateRange(Integer userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> result = new LinkedHashMap<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            // 直接用 repo 聚合总热量
            Integer total = foodListRepository.sumCurrentKcalByDate(userId, current);
            result.put(current.toString(), total != null ? total : 0);
            current = current.plusDays(1);
        }
        return result;
    }
}