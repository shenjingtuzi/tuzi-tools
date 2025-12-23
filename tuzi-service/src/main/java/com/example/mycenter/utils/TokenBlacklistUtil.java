package com.example.mycenter.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Token 黑名单工具类（登出后加入黑名单，禁止复用）
 */
public class TokenBlacklistUtil {
    // 存储已失效的 Token（单机版，生产环境替换为 Redis）
    private static final Set<String> TOKEN_BLACKLIST = new HashSet<>();

    /**
     * 将 Token 加入黑名单
     */
    public static void addToBlacklist(String token) {
        TOKEN_BLACKLIST.add(token);
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public static boolean isBlacklisted(String token) {
        return TOKEN_BLACKLIST.contains(token);
    }

    /**
     * 移除黑名单中的 Token（可选，用于 Token 过期后清理）
     */
    public static void removeFromBlacklist(String token) {
        TOKEN_BLACKLIST.remove(token);
    }
}