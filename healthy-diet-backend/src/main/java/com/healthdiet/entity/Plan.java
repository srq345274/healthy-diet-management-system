package com.healthdiet.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "plan")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDate date;

    @Column(name = "meal_type")
    private Integer mealType;

    @Column(name = "recipe_id")
    private Integer recipeId;

    @Column(name = "user_id")
    private Integer userId;
}