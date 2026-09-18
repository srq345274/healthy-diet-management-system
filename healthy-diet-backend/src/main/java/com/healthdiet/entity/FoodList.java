package com.healthdiet.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "list")
public class FoodList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "meal_type", nullable = false, length = 50)
    private String mealType;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "target_kcal")
    private Integer targetKcal = 500;

    @Column(name = "current_kcal")
    private Integer currentKcal = 0;
}