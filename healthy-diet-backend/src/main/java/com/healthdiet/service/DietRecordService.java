package com.healthdiet.service;

import com.healthdiet.dto.SaveDietRecordRequest;
import com.healthdiet.entity.DietRecord;
import com.healthdiet.entity.Food;
import com.healthdiet.entity.FoodList;
import com.healthdiet.repository.DietRecordRepository;
import com.healthdiet.repository.FoodListRepository;
import com.healthdiet.repository.FoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DietRecordService {

    private final FoodRepository foodRepository;
    private final FoodListRepository listRepository;
    private final DietRecordRepository dietRecordRepository;

    /**
     * 新接口：直接保存 food + diet_record
     */
    @Transactional
    public void saveRecord(SaveDietRecordRequest req) {

        Food food = new Food();
        food.setName(req.getDishName());
        food.setCategory(req.getCategory());
        food.setImage(req.getImageBase64());
        food.setCalories(req.getCalories() != null ? req.getCalories() : 0);
        food.setCarbG(req.getCarbG() != null ? BigDecimal.valueOf(req.getCarbG()) : BigDecimal.ZERO);
        food.setProteinG(req.getProteinG() != null ? BigDecimal.valueOf(req.getProteinG()) : BigDecimal.ZERO);
        food.setFatG(req.getFatG() != null ? BigDecimal.valueOf(req.getFatG()) : BigDecimal.ZERO);
        food.setFiberG(req.getFiberG() != null ? BigDecimal.valueOf(req.getFiberG()) : BigDecimal.ZERO);
        food.setSugarG(req.getSugarG() != null ? BigDecimal.valueOf(req.getSugarG()) : BigDecimal.ZERO);
        food.setSodiumMg(req.getSodiumMg() != null ? BigDecimal.valueOf(req.getSodiumMg()) : BigDecimal.ZERO);
        food.setTips(req.getTips());

        food = foodRepository.save(food);

        DietRecord record = new DietRecord();
        record.setFoodId(food.getId());
        record.setListId(req.getListId());

        dietRecordRepository.save(record);
    }

    /**
     * 原接口：拍照识别后保存
     */
    @Transactional
    public void saveFromPhoto(
            Integer userId,
            Integer listId,
            String dishName,
            String imageBase64,
            String category,
            Integer calories,
            Double carbG,
            Double proteinG,
            Double fatG,
            Double fiberG,
            Double sugarG,
            Double sodiumMg,
            String tips
    ) {
        // 查找或创建 food
        Food food = foodRepository.findByName(dishName)
                .orElseGet(() -> {
                    Food f = new Food();
                    f.setName(dishName);
                    f.setImage(imageBase64);
                    f.setCategory(category);
                    f.setCalories(calories != null ? calories : 0);
                    f.setCarbG(carbG != null ? BigDecimal.valueOf(carbG) : BigDecimal.ZERO);
                    f.setProteinG(proteinG != null ? BigDecimal.valueOf(proteinG) : BigDecimal.ZERO);
                    f.setFatG(fatG != null ? BigDecimal.valueOf(fatG) : BigDecimal.ZERO);
                    f.setFiberG(fiberG != null ? BigDecimal.valueOf(fiberG) : BigDecimal.ZERO);
                    f.setSugarG(sugarG != null ? BigDecimal.valueOf(sugarG) : BigDecimal.ZERO);
                    f.setSodiumMg(sodiumMg != null ? BigDecimal.valueOf(sodiumMg) : BigDecimal.ZERO);
                    f.setTips(tips != null ? tips : "");
                    return foodRepository.save(f);
                });

        FoodList list = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("清单不存在"));

        DietRecord record = new DietRecord();
        record.setFoodId(food.getId());
        record.setListId(list.getId());
        dietRecordRepository.save(record);
    }
}