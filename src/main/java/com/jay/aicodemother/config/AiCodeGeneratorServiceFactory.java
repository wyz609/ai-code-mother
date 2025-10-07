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

import com.jay.aicodemother.ai.AiCodeGeneratorService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AiCodeGeneratorServiceFactory {

    private final ChatModel chatModel;
//    @Qualifier("openAiStreamingChatModel")
    private final StreamingChatModel streamingChatModel;

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}