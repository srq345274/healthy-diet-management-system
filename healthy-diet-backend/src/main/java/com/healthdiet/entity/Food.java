package com.healthdiet.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "food")
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String image;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private Integer calories;

    @Column(name = "carb_g", precision = 10, scale = 2)
    private BigDecimal carbG;

    @Column(name = "protein_g", precision = 10, scale = 2)
    private BigDecimal proteinG;

    @Column(name = "fat_g", precision = 10, scale = 2)
    private BigDecimal fatG;

    @Column(name = "fiber_g", precision = 10, scale = 2)
    private BigDecimal fiberG;

    @Column(name = "sugar_g", precision = 10, scale = 2)
    private BigDecimal sugarG;

    @Column(name = "sodium_mg", precision = 10, scale = 2)
    private BigDecimal sodiumMg;

    @Column(length = 1000)
    private String tips;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}