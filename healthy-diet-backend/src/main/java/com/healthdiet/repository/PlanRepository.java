package com.healthdiet.repository;

import com.healthdiet.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {

    // 查询一周计划
    List<Plan> findByUserIdAndDateBetween(
            Integer userId,
            LocalDate startDate,
            LocalDate endDate
    );

    // 查询某天某餐全部菜品
    List<Plan> findByUserIdAndDateAndMealType(
            Integer userId,
            LocalDate date,
            Integer mealType
    );

    List<Plan> findByDateAndUserId(LocalDate date, Integer userId);

    // 删除某天某餐全部菜品
    void deleteByUserIdAndDateAndMealType(
            Integer userId,
            LocalDate date,
            Integer mealType
    );
}