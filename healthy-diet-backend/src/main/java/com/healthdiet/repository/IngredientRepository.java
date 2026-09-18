package com.healthdiet.repository;

import com.healthdiet.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredientRepository
        extends JpaRepository<Ingredient, Integer> {

    List<Ingredient> findByRecipeIdIn(
            List<Integer> recipeIds
    );

}