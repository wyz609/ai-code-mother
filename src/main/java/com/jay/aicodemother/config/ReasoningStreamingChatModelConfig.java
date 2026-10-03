/**
 * Class name: ReasoningStreamingChatModelConfig
 * Package: com.jay.aicodemother.config
 * Description:
 *
 * @Create: 2025/10/22 10:08
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.reasoning-streaming-chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private int maxTokens;

    private double temperature;

    private boolean logRequests;

    private boolean logResponses;

    /**
     * 推理流式模型
     * 注意：代理通过 LangChain4jProxyConfig 配置 JVM 系统属性
     * @return
     */
    @Bean
    public StreamingChatModel reasoningStreamingChatModel(){
        // 记录模型初始化日志
        log.info("初始化推理流式模型，baseUrl: {}, modelName: {}", baseUrl, modelName);

        // 构建 OpenAI 兼容的推理流式模型（用于复杂推理场景）
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)         // API 密钥
                .modelName(modelName)   // 模型名称（如 deepseek-reasoner）
                .maxTokens(maxTokens)   // 最大生成 token 数
                .temperature(temperature) // 采样温度
                .baseUrl(baseUrl)       // API 基础地址
                .logRequests(logRequests) // 是否记录请求日志
                .logResponses(logResponses) // 是否记录响应日志
                .timeout(Duration.of(120, ChronoUnit.SECONDS)) // 增加超时时间到 120 秒
                .build();
    }
}