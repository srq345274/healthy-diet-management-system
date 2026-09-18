package com.healthdiet.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FoodProcessResult {
    private String stickerBase64;
    private String dishName;
    private String category;       // 食物/饮料
    private Integer calories;
    private BigDecimal carbG;
    private BigDecimal proteinG;
    private BigDecimal fatG;
    private BigDecimal fiberG;
    private BigDecimal sugarG;
    private BigDecimal sodiumMg;
    private String tips;
    private String ingredient;
}