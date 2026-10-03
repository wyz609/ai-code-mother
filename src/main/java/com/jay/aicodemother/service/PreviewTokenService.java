package com.jay.aicodemother.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 预览令牌服务：为生成项目的沙箱预览签发短期随机令牌。
 *
 * <p>业务说明：预览接口通过 token 鉴权，让生成的页面只能访问自己应用的构建产物，
 * 无需携带用户会话，避免越权。令牌 10 分钟过期。</p>
 */
@Service
public class PreviewTokenService {

    // 令牌缓存：token → appId，最多 1000 个，写入后 10 分钟过期
    private final Cache<String, Long> tokenCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    /**
     * 为指定应用创建短期预览令牌
     */
    public String createToken(Long appId) {
        // 生成 32 位随机 UUID（去掉中划线）
        String token = UUID.randomUUID().toString().replace("-", "");
        // 绑定 token 到应用 ID 存入缓存
        tokenCache.put(token, appId);
        // 返回令牌
        return token;
    }

    /**
     * 校验令牌是否对该应用有效（未过期且绑定的应用一致）
     */
    public boolean isValidForApp(String token, Long appId) {
        // 令牌与应用 ID 都非空，且缓存中绑定的应用匹配
        return token != null && appId != null && appId.equals(tokenCache.getIfPresent(token));
    }
}
