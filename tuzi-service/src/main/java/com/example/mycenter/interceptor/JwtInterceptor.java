package com.example.mycenter.interceptor;

import com.example.mycenter.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取token
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("未登录，请先登录");
        }
        // 2. 验证token是否过期
        if (jwtUtil.isTokenExpired(token)) {
            throw new RuntimeException("token已过期，请重新登录");
        }
        // 3. 解析token，将userId存入request（供后续接口使用）
        Claims claims = jwtUtil.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        request.setAttribute("userId", userId);
        return true;
    }
}