package com.healthdiet.controller;

import com.healthdiet.dto.FoodProcessResult;
import com.healthdiet.dto.ProcessFoodRequest;
import com.healthdiet.dto.SaveDietRecordRequest;
import com.healthdiet.entity.User;
import com.healthdiet.repository.UserRepository;
import com.healthdiet.service.DietRecordService;
import com.healthdiet.service.FoodProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/diet-records")
@RequiredArgsConstructor
public class DietRecordController {

    private final FoodProcessService foodProcessService;
    private final DietRecordService dietRecordService;
    private final UserRepository userRepository;

    /**
     * AI识别食物（只识别，不保存）
     */
    @PostMapping("/process-food")
    public ResponseEntity<FoodProcessResult> processFood(
            @RequestBody ProcessFoodRequest req) {

        FoodProcessResult result =
                foodProcessService.process(req.getImageBase64());

        return ResponseEntity.ok(result);
    }

    /**
     * AI识别食物并保存（只调用一次 AI）
     */
    @PostMapping("/from-photo")
    public ResponseEntity<?> fromPhoto(
            @RequestHeader("token") String token,
            @RequestBody ProcessFoodRequest req) {

        User user = userRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("未登录"));

        // 调用 AI 只一次
        FoodProcessResult result = foodProcessService.process(req.getImageBase64());

        dietRecordService.saveFromPhoto(
                user.getId(),
                req.getListId(),
                result.getDishName(),
                result.getStickerBase64(),
                result.getCategory(), // 食物/饮料
                result.getCalories(),
                result.getCarbG() != null ? result.getCarbG().doubleValue() : null,
                result.getProteinG() != null ? result.getProteinG().doubleValue() : null,
                result.getFatG() != null ? result.getFatG().doubleValue() : null,
                result.getFiberG() != null ? result.getFiberG().doubleValue() : null,
                result.getSugarG() != null ? result.getSugarG().doubleValue() : null,
                result.getSodiumMg() != null ? result.getSodiumMg().doubleValue() : null,
                result.getTips()
        );

        return ResponseEntity.ok("保存成功");
    }

    /**
     * 新接口：直接保存 food + diet_record，不重复调用 AI
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveRecord(@RequestBody SaveDietRecordRequest req) {
        dietRecordService.saveRecord(req);
        return ResponseEntity.ok("保存成功");
    }
}