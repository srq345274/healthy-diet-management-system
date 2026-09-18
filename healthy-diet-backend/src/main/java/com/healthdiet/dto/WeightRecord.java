package com.healthdiet.dto;

import java.time.LocalDate;

public class WeightRecord {
    private LocalDate date;
    private Double weight;

    public WeightRecord() {}

    public WeightRecord(LocalDate date, Double weight) {
        this.date = date;
        this.weight = weight;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }
}