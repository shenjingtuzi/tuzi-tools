package com.example.mycenter.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mycenter.entity.User;
import com.example.mycenter.mapper.UserMapper;
import com.example.mycenter.utils.JwtUtil;
import com.example.mycenter.utils.WechatUtil;
import com.example.mycenter.vo.WxLoginRequest;
import com.example.mycenter.vo.WxLoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    @Autowired
    private WechatUtil wechatUtil;
    @Autowired
    private JwtUtil jwtUtil;

    // 微信登录核心逻辑
    public WxLoginResponse wxLogin(WxLoginRequest request) {
        try {
            // 1. 调用微信接口获取openid
            JSONObject wxSession = wechatUtil.getWxSession(request.getCode());
            String openid = wxSession.getString("openid");
            String unionid = wxSession.getString("unionid") == null ? "" : wxSession.getString("unionid");

            // 2. 根据openid查询用户（不存在则创建）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                    .eq(User::getOpenid, openid);
            User user = this.getOne(queryWrapper);
            boolean isNewUser = false;
            if (user == null) {
                // 新建用户
                user = new User();
                user.setOpenid(openid);
                user.setUnionid(unionid);
                user.setFreeCount(3); // 初始免费次数
                user.setIntegral(0);
                user.setIsVip(0); // 默认非会员
                user.setCreateTime(LocalDateTime.now());
                isNewUser = true;
            }

            // 3. 更新用户信息（昵称/头像/性别）
            if (request.getNickName() != null && !request.getNickName().isEmpty()) {
                user.setNickName(request.getNickName());
            }
            if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
                user.setAvatarUrl(request.getAvatarUrl());
            }
            if (request.getGender() != null) {
                user.setGender(request.getGender());
            }

            // 4. 生成JWT token并更新
            String token = jwtUtil.generateToken(user.getId(), openid);
            user.setToken(token);
            user.setTokenExpireTime(LocalDateTime.now().plusHours(2));
            user.setUpdateTime(LocalDateTime.now());

            // 5. 保存用户（新增/更新）
            this.saveOrUpdate(user);

            // 6. 构建返回结果
            WxLoginResponse response = new WxLoginResponse();
            response.setToken(token);
            response.setUserId(user.getId());
            response.setNickName(user.getNickName());
            response.setAvatarUrl(user.getAvatarUrl());
            response.setIsVip(user.getIsVip() == 1);
            // 格式化会员过期时间（非会员为空）
            if (user.getVipExpireTime() != null) {
                response.setVipExpireTime(user.getVipExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else {
                response.setVipExpireTime("");
            }
            response.setFreeCount(user.getFreeCount());

            return response;
        } catch (Exception e) {
            throw new RuntimeException("登录失败：" + e.getMessage());
        }
    }

    public void logout(Long userId) {
        // 1. 查询用户
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 2. 清空token和token过期时间（销毁登录态）
        user.setToken("");
        user.setTokenExpireTime(null);
        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
    }
}