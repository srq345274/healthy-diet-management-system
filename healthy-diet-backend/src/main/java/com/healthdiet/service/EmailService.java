package com.healthdiet.service;

public interface EmailService {
    // 发送6位邮箱验证码
    void sendVerifyCode(String email);
    // 校验验证码
    boolean checkCode(String email, String code);
}
