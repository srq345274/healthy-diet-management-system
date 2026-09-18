package com.healthdiet.service;

import com.healthdiet.entity.Ingredient;
import com.healthdiet.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    /**
     * 根据菜谱ID列表查询食材
     */
    public List<Ingredient> getIngredientsByRecipeIds(
            List<Integer> recipeIds
    ) {
        return ingredientRepository.findByRecipeIdIn(recipeIds);
    }
}