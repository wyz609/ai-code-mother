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
import com.jay.aicodemother.ai.tools.ToolManage;
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

    @Resource
    private ToolManage toolManage;
    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 100 个实例（减少内存占用）
     * - 写入后 15 分钟过期（缩短过期时间）
     * - 访问后 5 分钟过期（缩短访问过期时间）
     * - 使用弱值引用，允许 GC 在内存不足时回收
     * - 添加内存监控，当缓存占用过大时自动清理
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(100)  // 减少最大缓存数量，避免内存溢出
            .expireAfterWrite(Duration.ofMinutes(15))  // 缩短写入过期时间
            .expireAfterAccess(Duration.ofMinutes(5))   // 缩短访问过期时间
            .weakValues()  // 使用弱引用，允许 GC 在内存紧张时回收
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
                // 当实例被移除时，可以执行清理操作
                if (value != null) {
                    log.info("清理 AI 服务实例，释放资源，缓存键: {}", key);
                }
            })
            .recordStats()  // 开启统计，便于监控
            .build();

    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId){
        // 缺省按 HTML 类型获取服务实例
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 获取服务
     * @param appId 应用 ID
     * @param codeGenType 生成类型
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenType){
        // 默认启用文件工具
        return getAiCodeGeneratorService(appId, codeGenType, true);
    }

    /**
     * 首次生成静态页面时不向模型暴露文件工具，避免超长代码被塞入工具 JSON 参数后发生转义截断。
     * 已有项目的增量修改和 Vue 工程仍需要文件工具。
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenType,
                                                             boolean enableFileTools){
        // 构建缓存键（应用 ID + 类型 + 是否启用工具）
        String cacheKey = buildCacheKey(appId, codeGenType, enableFileTools);
        // 如果缓存中没有对应 Key 相应的实例， 则调用 createAiCodeGeneratorService 方法创建实例 并保存到缓存中供后续使用
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType, enableFileTools));
    }

    /**
     * 获取缓存统计信息，用于监控
     */
    public String getCacheStats() {
        // 格式化输出缓存大小、命中率、命中/未命中次数
        return String.format("AI服务缓存统计: 大小=%d, 命中率=%.2f%%, 命中次数=%d, 未命中次数=%d",
                serviceCache.estimatedSize(),
                serviceCache.stats().hitRate() * 100,
                serviceCache.stats().hitCount(),
                serviceCache.stats().missCount());
    }

    /**
     * 手动清理缓存（可在内存紧张时调用）
     */
    public void cleanUpCache() {
        // 执行缓存清理
        serviceCache.cleanUp();
        // 记录清理后缓存大小
        log.info("AI 服务缓存已清理，当前大小: {}", serviceCache.estimatedSize());
    }

    /**
     * 构建缓存键：appId_类型_是否启用工具
     */
    private String buildCacheKey(Long appId, CodeGenTypeEnum codeGenType, boolean enableFileTools) {
        return appId + "_" + codeGenType.getValue() + "_" + (enableFileTools ? "tools" : "text");
    }

    /**
     * 创建 AI 服务实例
     * @param appId
     * @param codeGenType
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenType,
                                                                  boolean enableFileTools){
        // 记录创建日志
        log.info("创建 AI 服务实例， appId : {}, 文件工具: {}", appId, enableFileTools);
        // 根据 appId 创建独立的对话记忆（每条应用独立上下文）
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                // 使用 Redis 存储聊天记忆（支持分布式共享）
                .chatMemoryStore(redisChatMemoryStore)
                // 最多保留 100 条消息
                .maxMessages(100)
                .build();
        try {
            // 从数据库中加载历史对话到记忆中（最多 20 条）
            chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        } catch (Exception e) {
            // 加载失败则使用空记忆
            log.error("加载聊天历史记录时出错，将使用空的记忆实例: ", e);
        }
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType)
                {
                    // Vue 项目需要连续流式输出并频繁调用文件工具，使用普通聊天模型保证兼容性。
                    // 推理模型在长工具链请求中可能只返回 reasoning/usage 事件，最终没有可用正文。
                    case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                            .chatModel(chatModel) // 默认模型
                            .streamingChatModel(openAiStreamingChatModel)
                            .chatMemoryProvider(memory -> chatMemory)
                            .tools(toolManage.getTools())
                            .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(toolExecutionRequest,"Error: this is not tool called"
                                    + toolExecutionRequest.name())) // 幻觉工具名称策略， 配置了不同的工具时的处理策略， 让框架帮我们处理 AI 出现幻觉的情况， 否则调用对话方法可能会报错
                            .build();
                    // HTML/多文件：可选启用文件工具
                    case MULTI_FILE,HTML -> {
                        // 构建服务实例（聊天模型 + 流式模型 + 按应用记忆）
                        var builder = AiServices.builder(AiCodeGeneratorService.class)
                                .chatModel(chatModel)
                                .streamingChatModel(openAiStreamingChatModel)
                                // 方法使用 @MemoryId，必须通过 provider 按 appId 提供聊天记忆。
                                .chatMemoryProvider(memory -> chatMemory);
                        // 仅在需要时挂载文件工具
                        if (enableFileTools) {
                            builder.tools(toolManage.getTools());
                        }
                        // 返回构建结果
                        yield builder.build();
                    }
                    // 其他类型不支持
                    default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型: " + codeGenType.getValue());
                };
    }

    /**
     * 创建 AI 服务实例（默认应用 ID=0，用于通用 Bean）
     * @param appId
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId){
        // 记录创建日志
        log.info("创建 AI 服务实例， appId : {}", appId);
        // 根据 appId 创建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                // Redis 存储
                .chatMemoryStore(redisChatMemoryStore)
                // 最多 20 条消息
                .maxMessages(20)
                .build();
        try {
            // 从数据库中加载历史对话到记忆中
            chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        } catch (Exception e) {
            // 加载失败使用空记忆
            log.error("加载聊天历史记录时出错，将使用空的记忆实例: ", e);
        }
        // 构建基础 AI 服务实例
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(openAiStreamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 暴露默认 AI 代码生成服务 Bean（通用实例）
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        // 使用默认应用 ID 创建通用实例
        return getAiCodeGeneratorService(0L);
    }

}
