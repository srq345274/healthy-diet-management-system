package com.healthdiet.dto;

import lombok.Data;

import java.util.List;

@Data
public class FoodListDetailDTO {

    private Integer id;
    private String name;           // mealType
    private Integer targetKcal;
    private Integer currentKcal;
    private List<FoodDTO> foods;   // 食物列表

    @Data
    public static class FoodDTO {
        private Integer id;
        private String name;
        private String image;
        private Integer calories;
    }
}