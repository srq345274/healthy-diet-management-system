package com.healthdiet.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "health_data")
public class HealthData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    private Double height;

    private Double weight;

    @Column(columnDefinition = "TEXT")
    private String disease;

    @Column(name = "avoid_food", columnDefinition = "TEXT")
    private String avoidFood;

    @Column(columnDefinition = "TEXT")
    private String allergy;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}