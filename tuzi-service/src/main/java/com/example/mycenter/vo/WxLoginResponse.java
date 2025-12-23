package com.example.mycenter.vo;

import lombok.Data;

@Data
public class WxLoginResponse {
    private String token;
    private Long userId;
    private String nickName;
    private String avatarUrl;
    private Boolean isVip;
    private String vipExpireTime; // 格式化后的时间字符串
    private Integer freeCount;
}
