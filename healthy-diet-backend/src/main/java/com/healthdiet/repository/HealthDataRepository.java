package com.healthdiet.repository;

import com.healthdiet.entity.HealthData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthDataRepository extends JpaRepository<HealthData, Integer> {

    Optional<HealthData> findTopByUserIdOrderByCreateTimeDesc(Integer userId);

    List<HealthData> findByUserIdAndCreateTimeBetweenOrderByCreateTimeAsc(
            Integer userId,
            LocalDateTime start,
            LocalDateTime end
    );
}