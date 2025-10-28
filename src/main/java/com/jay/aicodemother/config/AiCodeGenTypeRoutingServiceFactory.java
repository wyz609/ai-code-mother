package com.jay.aicodemother.config;

import com.jay.aicodemother.ai.AiCodeGenTypeRoutingService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Class name: AiCodeGenTypeRoutingServiceFactory
 * Package: com.jay.aicodemother.config
 * Description:
 *
 * @Create: 2025/10/27 20:36
 * @Author: jay
 * @Version: 1.0
 */

@Slf4j
@Configuration
public class AiCodeGenTypeRoutingServiceFactory {

    @Resource
    private ChatModel chatModel;

    /**
     * 创建 AI 代码生成类型路由服务实例
     * @return
     */
    @Bean
    public AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService() {
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(chatModel)
                .build();
    }
}