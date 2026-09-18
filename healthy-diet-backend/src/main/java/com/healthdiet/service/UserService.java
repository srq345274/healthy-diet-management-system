package com.healthdiet.service;

import com.healthdiet.dto.*;
import com.healthdiet.entity.User;
import com.healthdiet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    // 注册
    @Transactional
    public TokenResponse register(EmailRegisterDTO dto) {

        if (!emailService.checkCode(dto.getEmail(), dto.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        if (userRepository.existsByQqEmail(dto.getEmail())) {
            throw new RuntimeException("邮箱已注册");
        }

        User user = new User();

        user.setQqEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setNickname(dto.getNickname());

        if (dto.getBirthday() != null && !dto.getBirthday().isEmpty()) {
            user.setBirthday(
                    LocalDate.parse(dto.getBirthday(), DateTimeFormatter.ISO_DATE)
            );
        }

        String token = UUID.randomUUID().toString();

        user.setToken(token);

        userRepository.save(user);

        return new TokenResponse(token);
    }

    // 密码登录
    @Transactional
    public TokenResponse emailPwdLogin(EmailPwdLoginDTO dto) {

        User user = userRepository.findByQqEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!user.getPassword().equals(dto.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        String token = UUID.randomUUID().toString();

        user.setToken(token);

        userRepository.save(user);

        return new TokenResponse(token);
    }

    // 验证码登录
    @Transactional
    public TokenResponse emailCodeLogin(EmailCodeLoginDTO dto) {

        if (!emailService.checkCode(dto.getEmail(), dto.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        User user = userRepository.findByQqEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String token = UUID.randomUUID().toString();

        user.setToken(token);

        userRepository.save(user);

        return new TokenResponse(token);
    }

    // 重置密码
    @Transactional
    public void emailResetPwd(EmailResetPwdDTO dto) {

        if (!emailService.checkCode(dto.getEmail(), dto.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        User user = userRepository.findByQqEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setPassword(dto.getNewPassword());

        userRepository.save(user);
    }

    // 获取用户信息
    public UserInfoDTO getUserInfoByToken(String token) {

        User user = userRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("未登录或 Token 无效"));

        UserInfoDTO dto = new UserInfoDTO();

        dto.setNickname(user.getNickname());
        dto.setQqEmail(user.getQqEmail());
        dto.setAvatar(user.getAvatar());

        if (user.getGender() != null) {
            dto.setGender(user.getGender().toString());
        }

        if (user.getBirthday() != null) {
            dto.setBirthday(user.getBirthday().toString());
        }

        return dto;
    }

    // 更新用户资料
    @Transactional
    public void updateProfile(String token, String field, String value) {

        User user = userRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("未登录或 Token 无效"));

        switch (field) {

            case "nickname":
                user.setNickname(value);
                break;

            case "avatar":
                user.setAvatar(value);
                break;

            case "gender":
                user.setGender(Integer.valueOf(value));
                break;

            default:
                throw new RuntimeException("未知字段");
        }

        userRepository.save(user);
    }

    // 检查邮箱是否存在
    public boolean existsByEmail(String email) {
        return userRepository.existsByQqEmail(email);
    }
}