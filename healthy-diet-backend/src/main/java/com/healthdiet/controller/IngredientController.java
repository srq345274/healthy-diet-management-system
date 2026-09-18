package com.healthdiet.controller;

import com.healthdiet.common.ApiResponse;
import com.healthdiet.entity.Ingredient;
import com.healthdiet.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingredient")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    /**
     * 根据菜谱ID列表查询食材
     *
     * GET:
     * /ingredient/list?recipeIds=1,2,3
     */
    @GetMapping("/list")
    public ApiResponse<List<Ingredient>> getIngredients(
            @RequestParam List<Integer> recipeIds
    ) {

        return ApiResponse.ok(
                ingredientService.getIngredientsByRecipeIds(recipeIds)
        );
    }
}