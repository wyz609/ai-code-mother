package com.jay.aicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAspectJAutoProxy(exposeProxy = true) // 开启切面编程
@MapperScan("com.jay.aicodemother.mapper") // 扫描 Mapper 接口
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
public class AiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeMotherApplication.class, args);
    }

}
