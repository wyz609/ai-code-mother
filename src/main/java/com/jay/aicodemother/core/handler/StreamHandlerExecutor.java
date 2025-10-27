package com.jay.aicodemother.core.handler;

import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import com.jay.aicodemother.service.ChatHistoryService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Class name: StreamHandlerExecutor
 * Package: com.jay.aicodemother.core.handler
 * Description: 流式处理器执行器
 *  根据代码生成的类型创建合适的流式处理器
 *  - 传统的 Flux<String> 流(Html,Multi_File) --> SimpleTextStreamHandler
 *  - TokenStream 格式的复杂流式输出(Vue_Project) --> JsonMessageStreamHandler
 *
 * @Create: 2025/10/25 22:10
 * @Author: jay
 * @Version: 1.0
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamHandlerExecutor {

    private final JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 创建流式处理器并处理聊天历史
     * @param originFlux
     * @param chatHistoryService
     * @param appId
     * @param loginUser
     * @param codeGenType
     * @return
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  long appId,
                                  User loginUser, CodeGenTypeEnum codeGenType){
        return switch (codeGenType){
            case VUE_PROJECT -> // 使用注入的组件实例
                    jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
            case HTML, MULTI_FILE -> // 简单文本处理器不需要依赖注入
                    new SimpleTextStreamHandler().handle(originFlux, chatHistoryService, appId, loginUser);
        };
    }

}