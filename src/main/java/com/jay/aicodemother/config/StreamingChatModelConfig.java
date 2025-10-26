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
//import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.model.openai.OpenAiChatModel;
//import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class StreamingChatModelConfig {
//
//    @Bean("openAiStreamingChatModel")
//    public OpenAiStreamingChatModel openAiStreamingChatModel() {
//        return OpenAiStreamingChatModel.builder()
//                .baseUrl("https://api.openai.com/v1")
//                .apiKey(System.getenv().getOrDefault("OPENAI_API_KEY", "your-api-key-here"))
//                .modelName("gpt-4o-mini")
//                .logRequests(false)
//                .logResponses(false)
//                .httpClientBuilder(dev.langchain4j.http.client.jdk.JdkHttpClientBuilderFactory.INSTANCE)
//                .build();
//    }
//
//    @Bean
//    public ChatModel chatModel() {
//        return OpenAiChatModel.builder()
//                .baseUrl("https://api.openai.com/v1")
//                .apiKey(System.getenv().getOrDefault("OPENAI_API_KEY", "your-api-key-here"))
//                .modelName("gpt-4o-mini")
//                .logRequests(false)
//                .logResponses(false)
//                .httpClientBuilder(dev.langchain4j.http.client.jdk.JdkHttpClientBuilderFactory.INSTANCE)
//                .build();
//    }
//}