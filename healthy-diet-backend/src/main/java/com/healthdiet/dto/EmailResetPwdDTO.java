package com.healthdiet.dto;

import lombok.Data;

@Data
public class EmailResetPwdDTO {

    private String email;

    private String code;

    private String newPassword;
}