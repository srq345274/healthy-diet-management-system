package com.healthdiet.dto;

import lombok.Data;

@Data
public class EmailCodeLoginDTO {

    private String email;

    private String code;
}