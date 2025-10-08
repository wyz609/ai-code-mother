/**
 * Class name: AiCodeGeneratorServiceFactory
 * Package: com.jay.aicodemother.config
 * Description:
 * 这个工厂类的主要作用是创建一个AiCodeGeneratorService的实例，该服务可以利用AI模型生成代码。通过Spring的依赖注入机制，它将已配置好的ChatModel注入到AiCodeGeneratorService中，使得该服务可以直接与AI模型交互。
 * 这种设计模式的优势：
 * 解耦了AI服务接口与具体实现
 * 利用Spring容器管理Bean的生命周期
 * 通过LangChain的AiServices动态生成服务实现，简化了开发过程
 * 保证了ChatModel的单例性和可重用性
 * 简单来说，这个配置类就是为应用程序提供一个可以调用AI生成代码功能的服务Bean。
 *
 * @Create: 2025/9/22 15:54
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jay.aicodemother.ai.AiCodeGeneratorService;
import com.jay.aicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AiCodeGeneratorServiceFactory {

    private final ChatModel chatModel;
//    @Qualifier("openAiStreamingChatModel")
    private final StreamingChatModel streamingChatModel;

    private final RedisChatMemoryStore redisChatMemoryStore;

    private final ChatHistoryService chatHistoryService;

    /**
     * AI 服务实例缓存
     *  缓存策略
     *  最大缓存 1000 个实例
     *  缓存过期时间 30 分钟
     *  缓存访问时间 10 分钟
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除， appId : {}, 原因 : {}", key, cause);
            })
            .build();

    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId){
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 创建 AI 服务实例
     * @param appId
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId){
        log.info("创建 AI 服务实例， appId : {}", appId);
        // 根据 appId 创建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库中加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        return getAiCodeGeneratorService(0L);
    }
}