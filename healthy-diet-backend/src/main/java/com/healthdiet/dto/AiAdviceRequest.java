package com.healthdiet.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AiAdviceRequest {
    @NotBlank
    private String question;

    /** 可选：把用户档案摘要一并给大模型 */
    private String profileSummary;

    /** 为 true 时在服务端自动附加当前登录用户的健康档案摘要 */
    private Boolean useProfile;
}