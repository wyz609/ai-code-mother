package com.jay.aicodemother.core.handler;

import cn.hutool.core.util.StrUtil;
import com.jay.aicodemother.model.entity.ChatHistory;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.jay.aicodemother.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Class name: SimpleTextStreamHandler
 * Package: com.jay.aicodemother.core.handler
 * Description: 简单文本流处理器 处理 HTML 和 MULTI_FILE 类型的流式响应
 *
 * @Create: 2025/10/25 21:40
 * @Author: jay
 * @Version: 1.0
 */
@Slf4j
@Component
public class SimpleTextStreamHandler {

    /**
     * 处理传统流式响应 (HTML 和 MULTI_FILE)
     * 直接收集完整的文本响应
     * @param originFlux 原始流
     * @param chatHistoryService 对话历史服务
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux, ChatHistoryService chatHistoryService,
                               long appId, User loginUser){
        // 创建流收集器（负责累积文本并落库）
        StreamCollector collector = new StreamCollector(chatHistoryService, appId, loginUser);
        // 逐块收集内容，完成/出错时回调收集器
        return originFlux.doOnNext(collector::collect)
                .doOnComplete(collector::onComplete)
                .doOnError(collector::onError);
    }

    /**
     * 流收集器：累积响应文本，在流结束时保存为对话历史
     */
    private static class StreamCollector {
        // 累积 AI 响应文本
        private final StringBuilder aiResponseBuilder = new StringBuilder();
        // 对话历史服务
        private final ChatHistoryService chatHistoryService;
        // 应用 ID
        private final long appId;
        // 登录用户
        private final User loginUser;

        public StreamCollector(ChatHistoryService chatHistoryService, long appId, User loginUser) {
            this.chatHistoryService = chatHistoryService;
            this.appId = appId;
            this.loginUser = loginUser;
        }

        public void collect(String chunk) {
            // 收集 AI 响应内容
            aiResponseBuilder.append(chunk);
        }

        public void onComplete() {
            // 添加 AI 响应内容到对话历史
            String aiResponse = aiResponseBuilder.toString();
            // 非空响应才保存
            if (StrUtil.isNotBlank(aiResponse)) {
                // 保存为 AI 类型历史消息
                chatHistoryService.addChatMessage(appId, aiResponse,
                        ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
            }
        }

        public void onError(Throwable throwable) {
            // 添加错误信息到对话历史
            String errorMessage = "AI 回复失败" + throwable.getMessage();
            // 保存为 ERROR 类型历史消息
            chatHistoryService.addChatMessage(appId, errorMessage,
                    ChatHistoryMessageTypeEnum.ERROR.getValue(), loginUser.getId());
        }
    }

}