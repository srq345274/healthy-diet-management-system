package com.healthdiet.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "qq_email", nullable = false, unique = true)
    private String qqEmail;

    @Column(nullable = false)
    private String password;

    private String nickname;

    private String avatar;

    @Column(length = 100)
    private String token;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    private Integer gender;

    private LocalDate birthday;
}