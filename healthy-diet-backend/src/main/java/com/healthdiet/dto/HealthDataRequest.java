package com.healthdiet.dto;

import lombok.Data;

import java.util.List;

@Data
public class HealthDataRequest {

    private Double height;

    private Double weight;

    private List<String> disease;

    private List<String> allergy;

    private List<String> avoidFood;
}