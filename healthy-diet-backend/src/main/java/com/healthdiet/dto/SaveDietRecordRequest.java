package com.healthdiet.dto;

import lombok.Data;

@Data
public class SaveDietRecordRequest {

    private Integer listId;
    private String dishName;
    private String category;
    private String imageBase64;
    private Integer calories;
    private Double carbG;
    private Double proteinG;
    private Double fatG;
    private Double fiberG;
    private Double sugarG;
    private Double sodiumMg;
    private String tips;
}