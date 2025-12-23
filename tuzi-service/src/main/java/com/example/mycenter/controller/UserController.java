package com.example.mycenter.controller;

import com.example.common.Result;
import com.example.mycenter.service.UserService;
import com.example.mycenter.utils.JwtUtil;
import com.example.mycenter.utils.TokenBlacklistUtil;
import com.example.mycenter.vo.WxLoginRequest;
import com.example.mycenter.vo.WxLoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    // 微信登录接口
    @PostMapping("/wxLogin")
    public Result<WxLoginResponse> wxLogin(@Validated @RequestBody WxLoginRequest request) {
        try {
            WxLoginResponse response = userService.wxLogin(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }

    /**
     * 登出接口
     * @param token 前端请求头中携带的 Token
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<Result> logout(@RequestHeader("token") String token) {
        try {
            // 1. 校验 Token 是否为空
            if (token == null || token.isEmpty()) {
                return Result.failure("Token 不能为空");
            }

            // 2. 解析 Token（验证 Token 有效性）
            jwtUtil.parseToken(token);

            // 3. 将 Token 加入黑名单（核心：失效 Token）
            TokenBlacklistUtil.addToBlacklist(token);

            // 4. 返回登出成功
            return Result.success(null);
        } catch (RuntimeException e) {
            // 处理 Token 已失效/过期的情况
            return Result.failure("登出成功（Token 已失效）");
        } catch (Exception e) {
            // 其他异常
            e.printStackTrace();
            return Result.failure("登出失败：" + e.getMessage());
        }
    }
}