///**
// * Class name: StreamingChatModelConfig
// * Package: com.jay.aicodemother.config
// * Description:
// *
// * @Create: 2025/9/28 17:23
// * @Author: jay
// * @Version: 1.0
// */
//package com.jay.aicodemother.config;
//
//import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
//import lombok.Data;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//@Data
//@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
//public class StreamingChatModelConfig {
//
//    private String apiKey;
//
//    private String baseUrl;
//
//    private String modelName;
//
//    private int maxTokens;
//
//    private boolean logRequests;
//
//    private boolean logResponses;
//
//    @Bean("openAiStreamingChatModel")
//    public OpenAiStreamingChatModel openAiStreamingChatModel() {
//        return OpenAiStreamingChatModel.builder()
//                .baseUrl(baseUrl)
//                .apiKey(apiKey)
//                .modelName(modelName)
//                .logRequests(logRequests)
//                .logResponses(logResponses)
//                .build();
//    }
//}