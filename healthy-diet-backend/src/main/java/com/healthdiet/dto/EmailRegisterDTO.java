package com.healthdiet.dto;

import lombok.Data;

@Data
public class EmailRegisterDTO {

    private String email;

    private String code;

    private String nickname;

    private String password;

    private String birthday;
}