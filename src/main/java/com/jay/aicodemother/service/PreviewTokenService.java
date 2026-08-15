package com.jay.aicodemother.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Issues short-lived tokens for generated-project preview resources.
 */
@Service
public class PreviewTokenService {

    private final Cache<String, Long> tokenCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    public String createToken(Long appId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenCache.put(token, appId);
        return token;
    }

    public boolean isValidForApp(String token, Long appId) {
        return token != null && appId != null && appId.equals(tokenCache.getIfPresent(token));
    }
}
