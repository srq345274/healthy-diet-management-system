package com.healthdiet.controller;

import com.healthdiet.common.ApiResponse;
import com.healthdiet.dto.*;
import com.healthdiet.service.EmailService;
import com.healthdiet.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserAuthController {

    private final EmailService emailService;
    private final UserService userService;

    public UserAuthController(EmailService emailService, UserService userService) {
        this.emailService = emailService;
        this.userService = userService;
    }

    // 1. 发送邮箱验证码（兼容 URL 参数和 JSON 请求体）
    @PostMapping("/email/send-code")
    public ApiResponse<?> sendCode(
            @RequestParam(value = "email", required = false) String emailParam,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String email = emailParam;
            if ((email == null || email.isEmpty()) && body != null) {
                email = body.get("email");
            }

            if (email == null || email.isEmpty()) {
                return ApiResponse.fail(-1, "邮箱不能为空");
            }

            emailService.sendVerifyCode(email);
            return ApiResponse.ok("验证码发送成功，请查收邮箱");
        } catch (Exception e) {
            e.printStackTrace(); // 打印异常方便调试
            return ApiResponse.fail(-1, "发送验证码失败：" + e.getMessage());
        }
    }

    // 2. 邮箱注册
    @PostMapping("/auth/email-register")
    public ApiResponse<TokenResponse> register(@RequestBody EmailRegisterDTO dto) {
        try {
            TokenResponse token = userService.register(dto);
            return ApiResponse.ok(token);
        } catch (Exception e) {
            return ApiResponse.fail(-1, e.getMessage());
        }
    }

    // 3. 邮箱密码登录
    @PostMapping("/auth/email-pwd-login")
    public ApiResponse<TokenResponse> pwdLogin(@RequestBody EmailPwdLoginDTO dto) {
        try {
            TokenResponse token = userService.emailPwdLogin(dto);
            return ApiResponse.ok(token);
        } catch (Exception e) {
            return ApiResponse.fail(-1, e.getMessage());
        }
    }

    // 4. 邮箱验证码登录
    @PostMapping("/auth/email-code-login")
    public ApiResponse<TokenResponse> codeLogin(@RequestBody EmailCodeLoginDTO dto) {
        try {
            TokenResponse token = userService.emailCodeLogin(dto);
            return ApiResponse.ok(token);
        } catch (Exception e) {
            return ApiResponse.fail(-1, e.getMessage());
        }
    }

    // 5. 邮箱重置密码
    @PostMapping("/auth/email-reset-pwd")
    public ApiResponse<?> resetPwd(@RequestBody EmailResetPwdDTO dto) {
        try {
            userService.emailResetPwd(dto);
            return ApiResponse.ok("密码重置成功");
        } catch (Exception e) {
            return ApiResponse.fail(-1, e.getMessage());
        }
    }

    // 6. 获取当前登录用户信息
    @GetMapping("/user/info")
    public ApiResponse<UserInfoDTO> getUserInfo(@RequestHeader("token") String token) {
        try {
            UserInfoDTO info = userService.getUserInfoByToken(token);
            return ApiResponse.ok(info);
        } catch (Exception e) {
            return ApiResponse.fail(-1, e.getMessage());
        }
    }

    // 7. 更新用户资料
    @PostMapping("/user/update-profile")
    public ApiResponse<?> updateProfile(
            @RequestHeader("token") String token,
            @RequestBody Map<String, Object> body) {
        try {
            userService.updateProfile(token,
                    (String) body.get("field"),
                    (String) body.get("value"));
            return ApiResponse.ok("更新成功");
        } catch (Exception e) {
            return ApiResponse.fail(-1, e.getMessage());
        }
    }

    // 8. 检查邮箱是否已注册
    @PostMapping("/email/check-email-exists")
    public ApiResponse<Boolean> checkEmailExists(@RequestBody Map<String, String> map) {
        String email = map.get("email");
        boolean exists = userService.existsByEmail(email);
        return ApiResponse.ok(exists);
    }
}