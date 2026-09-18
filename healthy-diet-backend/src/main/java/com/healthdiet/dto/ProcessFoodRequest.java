package com.healthdiet.dto;

import lombok.Data;

@Data
public class ProcessFoodRequest {
    private String imageBase64;
    private Integer listId;   // 当前清单ID
}