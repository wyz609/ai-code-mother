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
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    /**
     * 推理流式模型
     * @return
     */
    public StreamingChatModel reasoningStreamingChatModel(){
        // 按理来说工程化项目应该使用推理模型来生成代码，但是这里为了演示，使用 简单模型
        final String modelName = "deepseek-chat";
        final int maxTokens = 8192;
        // 生产环境使用
        // final String modelName = "deepseek-reasoner";
        // final int maxTokens = 327688;

        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .baseUrl(baseUrl)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}