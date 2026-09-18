package com.healthdiet.controller;

import com.healthdiet.entity.Ingredient;
import com.healthdiet.entity.Plan;
import com.healthdiet.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@CrossOrigin
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/day-plan")
    public List<Plan> getDayPlan(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date,
            HttpServletRequest request) {

        Object userObj =
                request.getAttribute("currentUserId");

        if (userObj == null) {
            throw new RuntimeException("用户未登录");
        }

        Integer userId;

        if (userObj instanceof Integer) {
            userId = (Integer) userObj;
        } else {
            userId = Integer.valueOf(userObj.toString());
        }

        return shopService.getDayPlan(date, userId);
    }

    @PostMapping("/ingredients")
    public List<Ingredient> getIngredients(@RequestBody List<Integer> recipeIds) {
        return shopService.getIngredientsByRecipeIds(recipeIds);
    }
}