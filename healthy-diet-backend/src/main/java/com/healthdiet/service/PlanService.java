package com.healthdiet.service;

import com.healthdiet.entity.Plan;
import com.healthdiet.entity.Recipe;
import com.healthdiet.repository.PlanRepository;
import com.healthdiet.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final RecipeRepository recipeRepository;

    private final Random random = new Random();

    /**
     * 获取本周计划
     * 如果不存在则自动生成
     */
    @Transactional
    public List<Plan> getOrCreateWeekPlan(
            Integer userId,
            LocalDate weekStart,
            LocalDate weekEnd
    ) {

        List<Plan> plans =
                planRepository.findByUserIdAndDateBetween(
                        userId,
                        weekStart,
                        weekEnd
                );

        if (!plans.isEmpty()) {
            return plans;
        }

        System.out.println("开始生成周计划...");

        List<Recipe> recipes = recipeRepository.findAll();

        System.out.println("菜谱数量=" + recipes.size());

        LocalDate current = weekStart;

        while (!current.isAfter(weekEnd)) {

            generateBreakfast(userId, current, recipes);

            generateLunch(userId, current, recipes);

            generateDinner(userId, current, recipes);

            current = current.plusDays(1);
        }

        return planRepository.findByUserIdAndDateBetween(
                userId,
                weekStart,
                weekEnd
        );
    }

    /**
     * 早餐
     * 主食 + 水果 + 饮料
     */
    private void generateBreakfast(
            Integer userId,
            LocalDate date,
            List<Recipe> recipes
    ) {

        saveRandom(userId, date, 1,
                recipes,
                "STAPLE", "staple", "主食");

        saveRandom(userId, date, 1,
                recipes,
                "FRUIT", "fruit", "水果");

        saveRandom(userId, date, 1,
                recipes,
                "DRINK", "drink", "饮料");
    }

    /**
     * 午餐
     * 主食 + 菜品 + 汤
     */
    private void generateLunch(
            Integer userId,
            LocalDate date,
            List<Recipe> recipes
    ) {

        saveRandom(userId, date, 2,
                recipes,
                "STAPLE", "staple", "主食");

        saveRandom(userId, date, 2,
                recipes,
                "VEGETABLE", "vegetable", "蔬菜", "菜品");

        saveRandom(userId, date, 2,
                recipes,
                "SOUP", "soup", "汤", "汤类");
    }

    /**
     * 晚餐
     * 主食 + 菜品 + 汤
     */
    private void generateDinner(
            Integer userId,
            LocalDate date,
            List<Recipe> recipes
    ) {

        saveRandom(userId, date, 3,
                recipes,
                "STAPLE", "staple", "主食");

        saveRandom(userId, date, 3,
                recipes,
                "VEGETABLE", "vegetable", "蔬菜", "菜品");

        saveRandom(userId, date, 3,
                recipes,
                "SOUP", "soup", "汤", "汤类");
    }

    /**
     * 随机保存一道菜
     */
    private void saveRandom(
            Integer userId,
            LocalDate date,
            Integer mealType,
            List<Recipe> recipes,
            String... categories
    ) {

        List<Recipe> candidates = new ArrayList<>();

        for (Recipe recipe : recipes) {

            String category =
                    recipe.getCategory() == null
                            ? ""
                            : recipe.getCategory().trim();

            for (String c : categories) {

                if (category.equalsIgnoreCase(c)) {
                    candidates.add(recipe);
                    break;
                }
            }
        }

        if (candidates.isEmpty()) {

            System.out.println(
                    "未找到分类: "
                            + String.join(",", categories)
            );

            return;
        }

        Recipe recipe =
                candidates.get(
                        random.nextInt(candidates.size())
                );

        Plan plan = new Plan();

        plan.setUserId(userId);
        plan.setDate(date);
        plan.setMealType(mealType);
        plan.setRecipeId(recipe.getId());

        planRepository.save(plan);

        System.out.println(
                "生成计划 -> "
                        + date
                        + " meal="
                        + mealType
                        + " "
                        + recipe.getName()
        );
    }

    /**
     * 修改某餐
     */
    @Transactional
    public void updateMeal(
            Integer userId,
            LocalDate date,
            Integer mealType,
            List<Integer> recipeIds
    ) {

        planRepository.deleteByUserIdAndDateAndMealType(
                userId,
                date,
                mealType
        );

        for (Integer recipeId : recipeIds) {

            Plan plan = new Plan();

            plan.setUserId(userId);
            plan.setDate(date);
            plan.setMealType(mealType);
            plan.setRecipeId(recipeId);

            planRepository.save(plan);
        }
    }
}