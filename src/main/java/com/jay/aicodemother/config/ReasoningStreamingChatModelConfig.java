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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

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
     * @return
     */
    @Bean
    public StreamingChatModel reasoningStreamingChatModel(){
        // 按理来说工程化项目应该使用推理模型来生成代码，但是这里为了演示，使用 简单模型
//        final String modelName = "deepseek-chat";
//        final int maxTokens = 8192;
        // 生产环境使用
//         final String modelName = "deepseek-reasoner";
//         final int maxTokens = 327688;

        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .baseUrl(baseUrl)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(Duration.of(20, ChronoUnit.SECONDS))
                .build();
    }
}