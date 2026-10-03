package com.jay.aicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 零代码生成平台 启动类。
 *
 * <p>项目功能：用户输入提示词，AI 自动生成 HTML / 多文件 / Vue 工程代码，
 * 支持流式对话、文件工具调用、预览、部署、截图封面等完整链路。</p>
 */
@EnableAspectJAutoProxy(exposeProxy = true) // 开启切面编程（支持 @AuthCheck 权限校验）
@MapperScan("com.jay.aicodemother.mapper") // 扫描 Mapper 接口
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
public class AiCodeMotherApplication {

    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        SpringApplication.run(AiCodeMotherApplication.class, args);
    }

}
