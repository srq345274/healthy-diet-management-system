package com.healthdiet.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProfileResponse {

    private UserInfo user;

    private HealthInfo health;

    @Data
    public static class UserInfo {
        private Integer gender;
        private LocalDate birthday;
    }

    @Data
    public static class HealthInfo {
        private Double height;
        private Double weight;

        private List<String> disease;
        private List<String> allergy;
        private List<String> avoidFood;
    }
}