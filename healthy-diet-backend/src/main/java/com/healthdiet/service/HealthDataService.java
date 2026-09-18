package com.healthdiet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdiet.dto.HealthDataRequest;
import com.healthdiet.dto.ProfileResponse;
import com.healthdiet.entity.HealthData;
import com.healthdiet.entity.User;
import com.healthdiet.repository.HealthDataRepository;
import com.healthdiet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthDataService {

    private final HealthDataRepository healthDataRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ProfileResponse getProfile(Integer userId) throws Exception {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        HealthData health = healthDataRepository
                .findTopByUserIdOrderByCreateTimeDesc(userId)
                .orElse(null);

        ProfileResponse response = new ProfileResponse();

        ProfileResponse.UserInfo userInfo = new ProfileResponse.UserInfo();
        userInfo.setGender(user.getGender());
        userInfo.setBirthday(user.getBirthday());
        response.setUser(userInfo);

        ProfileResponse.HealthInfo healthInfo = new ProfileResponse.HealthInfo();

        if (health != null) {
            healthInfo.setHeight(health.getHeight());
            healthInfo.setWeight(health.getWeight());

            healthInfo.setDisease(health.getDisease() == null ? Collections.emptyList() :
                    objectMapper.readValue(health.getDisease(), new TypeReference<>() {}));

            healthInfo.setAllergy(health.getAllergy() == null ? Collections.emptyList() :
                    objectMapper.readValue(health.getAllergy(), new TypeReference<>() {}));

            healthInfo.setAvoidFood(health.getAvoidFood() == null ? Collections.emptyList() :
                    objectMapper.readValue(health.getAvoidFood(), new TypeReference<>() {}));
        }

        response.setHealth(healthInfo);
        return response;
    }

    public void save(Integer userId, HealthDataRequest request) throws Exception {
        HealthData health = new HealthData();
        health.setUserId(userId);
        health.setHeight(request.getHeight());
        health.setWeight(request.getWeight());
        health.setDisease(request.getDisease() == null ? null :
                objectMapper.writeValueAsString(request.getDisease()));
        health.setAllergy(request.getAllergy() == null ? null :
                objectMapper.writeValueAsString(request.getAllergy()));
        health.setAvoidFood(request.getAvoidFood() == null ? null :
                objectMapper.writeValueAsString(request.getAvoidFood()));
        health.setCreateTime(LocalDateTime.now());

        healthDataRepository.save(health);
    }

    /**
     * 获取用户体重历史记录
     *
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return List of WeightRecord
     */
    public List<WeightRecord> getWeightHistory(Integer userId, LocalDate startDate, LocalDate endDate) {

        List<HealthData> list =
                healthDataRepository.findByUserIdAndCreateTimeBetweenOrderByCreateTimeAsc(
                        userId,
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59)
                );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return list.stream()
                .filter(item -> item.getWeight() != null)
                .map(item -> new WeightRecord(
                        item.getCreateTime().toLocalDate().format(formatter),
                        item.getWeight()
                ))
                .collect(Collectors.toList());
    }

    // WeightRecord DTO
    public static class WeightRecord {
        private String date;
        private Double weight;

        public WeightRecord(String date, Double weight) {
            this.date = date;
            this.weight = weight;
        }

        public String getDate() {
            return date;
        }

        public Double getWeight() {
            return weight;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public void setWeight(Double weight) {
            this.weight = weight;
        }
    }
}