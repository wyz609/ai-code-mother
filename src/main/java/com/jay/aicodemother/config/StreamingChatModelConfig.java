/**
 * Class name: StreamingChatModelConfig
 * Package: com.jay.aicodemother.config
 * Description: 代码生成专用流式模型配置
 *
 * 说明：
 * 1. 该 Bean 名称与 langchain4j starter 自动配置的 Bean 同名，
 *    通过 spring.main.allow-bean-definition-overriding=true 覆盖 starter 默认实现。
 * 2. 代码生成必须使用支持工具调用（function calling）且 content 正常流式输出的模型
 *    （如 deepseek-chat）。推理类模型（deepseek-v4-pro / deepseek-reasoner）的思考内容
 *    走 reasoning_content 字段，当前 langchain4j 内部 Delta 模型不支持解析该字段，
 *    会导致流结束时空响应，请勿在此配置推理模型。
 *
 * @Create: 2025/9/28 17:23
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.config;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
@Configuration
@Data
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
public class StreamingChatModelConfig {

    private String apiKey;

    private String baseUrl;

    private String modelName;

    private int maxTokens;

    private double temperature;

    private boolean logRequests;

    private boolean logResponses;

    @Bean("openAiStreamingChatModel")
    public OpenAiStreamingChatModel openAiStreamingChatModel() {
        // 记录模型初始化日志
        log.info("初始化代码生成流式模型，baseUrl: {}, modelName: {}", baseUrl, modelName);

        // 构建 OpenAI 兼容的流式聊天模型（用于代码生成）
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)       // API 基础地址
                .apiKey(apiKey)         // API 密钥
                .modelName(modelName)   // 模型名称（如 deepseek-chat）
                .maxTokens(maxTokens)   // 最大生成 token 数
                .temperature(temperature) // 采样温度
                .logRequests(logRequests) // 是否记录请求日志
                .logResponses(logResponses) // 是否记录响应日志
                .timeout(Duration.of(300, ChronoUnit.SECONDS)) // Vue 项目工具链较长，放宽超时
                .build();
    }
}