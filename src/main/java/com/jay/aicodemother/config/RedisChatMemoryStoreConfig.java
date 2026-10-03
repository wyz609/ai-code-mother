package com.jay.aicodemother.config;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    // Redis 主机地址
    private String host;

    // Redis 端口
    private int port;

    // Redis 密码
    private String password;

    // 记忆过期时间（秒）
    private long ttl;

    /**
     * 创建 Redis 聊天记忆存储 Bean
     *
     * <p>业务说明：将 AI 对话记忆持久化到 Redis，支持多实例共享记忆、
     * 服务重启后对话上下文不丢失。</p>
     */
    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        // 构建 Redis 记忆存储（host/port/password/ttl 来自配置）
        RedisChatMemoryStore.Builder builder = RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                .ttl(ttl);
        // 设置了密码时指定默认用户名
        if (StrUtil.isNotBlank(password)) {
            builder.user("default");
        }
        // 构建并返回存储实例
        return builder.build();
    }
}
