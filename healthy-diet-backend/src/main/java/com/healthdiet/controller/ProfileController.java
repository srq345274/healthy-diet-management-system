package com.healthdiet.controller;

import com.healthdiet.dto.HealthDataRequest;
import com.healthdiet.dto.ProfileResponse;
import com.healthdiet.entity.User;
import com.healthdiet.repository.UserRepository;
import com.healthdiet.service.HealthDataService;
import com.healthdiet.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final HealthDataService healthDataService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<ProfileResponse> getProfile(
            @RequestHeader("Authorization") String authorization
    ) throws Exception {

        String token = authorization.replace("Bearer ", "");

        User user = userRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("未登录"));

        return ApiResponse.ok(
                healthDataService.getProfile(user.getId())
        );
    }

    @PutMapping
    public ApiResponse<Void> saveProfile(
            @RequestHeader("Authorization") String authorization,
            @RequestBody HealthDataRequest request
    ) throws Exception {

        String token = authorization.replace("Bearer ", "");

        User user = userRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("未登录"));

        healthDataService.save(user.getId(), request);

        return ApiResponse.ok(null);
    }

    /**
     * 体重变化趋势
     */
    @GetMapping("/statistics/weight-trend")
    public ApiResponse<List<HealthDataService.WeightRecord>> getWeightTrend(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String from,
            @RequestParam String to
    ) {

        String token = authorization.replace("Bearer ", "");

        User user = userRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("未登录"));

        LocalDate start = LocalDate.parse(from);
        LocalDate end = LocalDate.parse(to);

        return ApiResponse.ok(
                healthDataService.getWeightHistory(
                        user.getId(),
                        start,
                        end
                )
        );
    }

}
