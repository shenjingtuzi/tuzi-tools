package com.example.mycenter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id; // 用户ID
    private String openid; // 微信openid
    private String unionid; // 微信unionid
    private String nickName; // 昵称
    private String avatarUrl; // 头像
    private Integer gender; // 性别
    private String token; // JWT token
    private LocalDateTime tokenExpireTime; // token过期时间
    private Integer isVip; // 是否会员：0=否，1=是
    private String vipLevel; // 会员等级
    private LocalDateTime vipExpireTime; // 会员过期时间
    private Integer freeCount; // 剩余免费次数
    private Integer integral; // 积分
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}