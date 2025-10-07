///**
// * Class name: ReasoningStreamingChatModelConfig
// * Package: com.jay.aicodemother.config
// * Description:
// *
// * @Create: 2025/9/28 17:23
// * @Author: jay
// * @Version: 1.0
// */
//package com.jay.aicodemother.config;
//
//import dev.langchain4j.model.chat.StreamingChatModel;
//import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Scope;
//
//import java.time.Duration;
//import java.time.temporal.ChronoUnit;
//
//@Configuration
//@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
//public class StreamingChatModelConfig {
//
//    @Value("${base-url}")
//    String baseUrl;
//    @Value("${api-key}")
//    String apiKey;
//    @Value("${model-name}")
//    String modelName;
//    @Value("${max-tokens}")
//    Integer maxTokens;
//    @Value("${log-requests}")
//    Boolean logRequests;
//    @Value("${log-responses}")
//    Boolean logResponses;
//
//    @Bean
//    @Scope("prototype")
//    public StreamingChatModel StreamingChatModel() {
//        return OpenAiStreamingChatModel.builder()
//                .baseUrl(baseUrl)
//                .apiKey(apiKey)
//                .modelName(modelName)
//                .maxTokens(maxTokens)
//                .logRequests(logRequests)
//                .logResponses(logResponses)
//                .timeout(Duration.of(20, ChronoUnit.MILLIS))
//                .build();
//    }
//
//}