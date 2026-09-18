package com.healthdiet.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "recipe")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String intro;

    private String image;

    private Integer calories;
}