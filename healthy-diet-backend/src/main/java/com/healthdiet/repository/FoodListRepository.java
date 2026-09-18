package com.healthdiet.repository;

import com.healthdiet.entity.FoodList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FoodListRepository extends JpaRepository<FoodList, Integer> {

    // 根据用户、日期、餐次查询单条记录
    FoodList findByUserIdAndDateAndMealType(
            Integer userId,
            LocalDate date,
            String mealType
    );

    // 查询某用户某天的所有餐次
    List<FoodList> findByUserIdAndDate(
            Integer userId,
            LocalDate date
    );

    // 查询某用户某天的所有餐次并按 ID 升序
    List<FoodList> findByUserIdAndDateOrderByIdAsc(
            Integer userId,
            LocalDate date
    );

    // 统计某用户某天的总热量
    @Query(
            value = "SELECT IFNULL(SUM(current_kcal),0) " +
                    "FROM list " +
                    "WHERE user_id=:userId AND date=:date",
            nativeQuery = true
    )
    Integer sumCurrentKcalByDate(
            @Param("userId") Integer userId,
            @Param("date") LocalDate date
    );

    // 根据ID和用户ID查询单条清单，返回Optional
    @Query("SELECT f FROM FoodList f WHERE f.id = :id AND f.userId = :userId")
    Optional<FoodList> findByIdAndUserId(
            @Param("id") Integer id,
            @Param("userId") Integer userId
    );
}