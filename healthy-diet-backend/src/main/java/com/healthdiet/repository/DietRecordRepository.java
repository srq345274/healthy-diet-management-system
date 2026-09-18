package com.healthdiet.repository;

import com.healthdiet.entity.DietRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DietRecordRepository
        extends JpaRepository<DietRecord, Integer> {

    List<DietRecord> findByListId(Integer listId);

    List<DietRecord> findByFoodId(Integer foodId);

    long countByListId(Integer listId);

    void deleteByListId(Integer listId);
    List<DietRecord> findByListIdOrderByIdAsc(Integer listId);
}