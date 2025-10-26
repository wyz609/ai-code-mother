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
import com.jay.aicodemother.ai.tools.FileWriteTool;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import com.jay.aicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
//@RequiredArgsConstructor
@AllArgsConstructor
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private  ChatModel chatModel;

    @Resource
    private OpenAiStreamingChatModel openAiStreamingChatModel;

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;
    /**
     * AI 服务实例缓存
     *  缓存策略
     *  最大缓存 1000 个实例
     *  缓存过期时间 30 分钟
     *  缓存访问时间 10 分钟
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除， 缓存键 : {}, 原因 : {}", key, cause);
            })
            .build();

    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId){
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 获取服务
     * @param appId 应用 ID
     * @param codeGenType 生成类型
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenType){
        String cacheKey = buildCacheKey(appId, codeGenType);
        // 如果缓存中没有对应 Key 相应的实例， 则调用 createAiCodeGeneratorService 方法创建实例 并保存到缓存中供后续使用
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    private String buildCacheKey(Long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    /**
     * 创建 AI 服务实例
     * @param appId
     * @param codeGenType
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenType){
        log.info("创建 AI 服务实例， appId : {}", appId);
        // 根据 appId 创建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        try {
            // 从数据库中加载历史对话到记忆中
            chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        } catch (Exception e) {
            log.error("加载聊天历史记录时出错，将使用空的记忆实例: ", e);
        }
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType)
                {
                    // vue 项目生成使用推理模型
                    case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                            .chatModel(chatModel) // 默认模型
                            .streamingChatModel(reasoningStreamingChatModel)
                            .chatMemoryProvider(memory -> chatMemory)
                            .tools(new FileWriteTool())
                            .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(toolExecutionRequest,"Error: this is not tool called"
                                    + toolExecutionRequest.name())) // 幻觉工具名称策略， 配置了不同的工具时的处理策略， 让框架帮我们处理 AI 出现幻觉的情况， 否则调用对话方法可能会报错
                            .build();
                    case MULTI_FILE,HTML -> AiServices.builder(AiCodeGeneratorService.class)
                            .chatModel(chatModel)
                            .streamingChatModel(openAiStreamingChatModel)
                            .chatMemory(chatMemory)
                            .build();
                    default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型: " + codeGenType.getValue());
                };
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
        try {
            // 从数据库中加载历史对话到记忆中
            chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        } catch (Exception e) {
            log.error("加载聊天历史记录时出错，将使用空的记忆实例: ", e);
        }
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(openAiStreamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        return getAiCodeGeneratorService(0L);
    }

}