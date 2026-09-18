package com.healthdiet.dto;

import lombok.Data;

@Data
public class UserInfoDTO {

    private String nickname;

    private String qqEmail;

    private String avatar;

    private String gender;

    private String birthday;
}