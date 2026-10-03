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

    // 注入普通聊天模型（用于类型路由判断）
    @Resource
    private ChatModel chatModel;

    /**
     * 创建 AI 代码生成类型路由服务实例
     *
     * <p>业务说明：该服务通过 AI 分析用户提示词，自动判断应该使用
     * HTML / 多文件 / Vue 工程 哪种代码生成类型。</p>
     * @return
     */
    @Bean
    public AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService() {
        // 使用 AiServices 动态生成路由服务实现
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(chatModel)
                .build();
    }
}