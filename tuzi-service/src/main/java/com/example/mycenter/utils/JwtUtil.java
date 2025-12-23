package com.example.mycenter.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret; // 密钥
    @Value("${jwt.expire}")
    public Long expire; // 有效期（毫秒）

    // 生成token
    public String generateToken(Long userId, String openid) {
        // 构建密钥
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // 过期时间
        Date expireDate = new Date(System.currentTimeMillis() + expire);
        // 生成token（携带userId和openid）
        return Jwts.builder()
                .claim("userId", userId)
                .claim("openid", openid)
                .setExpiration(expireDate)
                .signWith(key)
                .compact();
    }

    // 解析token获取Claims
    public Claims parseToken(String token) {
        // 1. 先检查 Token 是否在黑名单中（登出后的 Token 直接失效）
        if (TokenBlacklistUtil.isBlacklisted(token)) {
            throw new RuntimeException("Token 已失效（用户已登出）");
        }
        // 2. 解析 Token
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 验证token是否过期
    public boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().before(new Date());
    }
}