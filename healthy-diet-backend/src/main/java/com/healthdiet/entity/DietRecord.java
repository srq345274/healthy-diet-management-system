package com.healthdiet.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "diet_record")
public class DietRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "food_id")
    private Integer foodId;

    @Column(name = "list_id")
    private Integer listId;

}