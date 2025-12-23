package com.example.mycenter.vo;

import lombok.Data;

@Data
public class WxLoginRequest {
    private String code;
    private String nickName;
    private String avatarUrl;
    private Integer gender = 0;
}
