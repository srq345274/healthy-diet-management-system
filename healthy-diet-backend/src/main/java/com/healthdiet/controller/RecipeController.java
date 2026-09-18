package com.healthdiet.controller;

import com.healthdiet.entity.Recipe;
import com.healthdiet.repository.RecipeRepository;
import com.healthdiet.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recipe")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeRepository recipeRepository;

    @GetMapping("/all")
    public ApiResponse<List<Recipe>> getAllRecipes() {
        List<Recipe> recipes = recipeRepository.findAll();
        return ApiResponse.ok(recipes);
    }
}