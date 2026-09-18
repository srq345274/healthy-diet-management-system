package com.healthdiet.service;

import com.healthdiet.entity.Ingredient;
import com.healthdiet.entity.Plan;
import com.healthdiet.repository.IngredientRepository;
import com.healthdiet.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final PlanRepository planRepository;
    private final IngredientRepository ingredientRepository;

    public List<Plan> getDayPlan(LocalDate date, Integer userId) {
        return planRepository.findByDateAndUserId(date, userId);
    }

    public List<Ingredient> getIngredientsByRecipeIds(List<Integer> recipeIds) {
        return ingredientRepository.findByRecipeIdIn(recipeIds);
    }
}