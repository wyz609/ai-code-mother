/**
 * Class name: AICodeGeneratorFacade
 * Package: com.jay.aicodemother.core
 * Description: AI代码生成门面类，提供统一的代码生成和保存接口
 *
 * @Create: 2025/9/22 22:35
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.core;

import cn.hutool.json.JSONUtil;
import com.jay.aicodemother.ai.AiCodeGeneratorService;
import com.jay.aicodemother.ai.model.HtmlCodeResult;
import com.jay.aicodemother.ai.model.MultiFileCodeResult;
import com.jay.aicodemother.ai.model.message.AIResponseMessage;
import com.jay.aicodemother.ai.model.message.ToolExecutedMessage;
import com.jay.aicodemother.ai.model.message.ToolRequestMessage;
import com.jay.aicodemother.config.AiCodeGeneratorServiceFactory;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import com.jay.aicodemother.parser.CodeParseExecutor;
import com.jay.aicodemother.save.CodeFileSaverExecutor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI代码生成门面类
 * 提供统一的接口来生成不同类型的代码并保存到文件系统中
 */
@Service
@Slf4j
public class AICodeGeneratorFacade {

    /**
     * AI代码生成服务，通过构造函数注入
     */
    @Resource
    private AiCodeGeneratorServiceFactory factory;


//    /**
//     * 获取AI代码生成服务工厂实例
//     * @return
//     */
//    private AiCodeGeneratorServiceFactory getAiCodeGeneratorServiceFactory() {
//        if (aiCodeGeneratorServiceFactory == null) {
//            try {
//                aiCodeGeneratorServiceFactory = applicationContext.getBean(AiCodeGeneratorServiceFactory.class);
//            } catch (Exception e) {
//                log.warn("无法从应用上下文获取AiCodeGeneratorServiceFactory bean: ", e);
//                return null;
//            }
//        }
//        return aiCodeGeneratorServiceFactory;
//    }

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用ID
     */
    public void generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        
        // 获取服务工厂实例
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = factory.getAiCodeGeneratorService(appId,codeGenTypeEnum);
        if (factory == null) {
            log.warn("AI代码生成服务工厂未初始化，无法生成代码");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI服务不可用");
        }
        switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        }
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }

        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = factory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        if (factory == null) {
            log.warn("AI代码生成服务工厂未初始化，无法生成代码");
            return Flux.just("错误：AI服务不可用");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream codeStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(codeStream);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>， 并传递工具调用信息 适配器类
     * @param codeStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream codeStream) {
        return Flux.create(sink -> {
            codeStream.onPartialResponse((String partialResponse) ->{
                AIResponseMessage aiResponseMessage = new AIResponseMessage(partialResponse);
                sink.next(JSONUtil.toJsonStr(aiResponseMessage));
            })
                    .onPartialToolExecutionRequest((Integer index, ToolExecutionRequest toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution)->{
                        ToolExecutedMessage toolExecutionMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutionMessage));
                    })
                    .onCompleteResponse((ChatResponse chatResponse) ->{
                        sink.complete();
                    })
                    .onError((Throwable error) ->{
                        error.printStackTrace();;
                        sink.error(error);
                    })
                    .start();
        });
    }

    /**
     * 处理流式代码生成
     *
     * @param codeStream 代码流
     * @param type       代码生成类型
     * @param appId      应用ID
     * @return 处理后的字符串流
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum type, Long appId) {
        // 收集流中的所有内容
        StringBuilder contentBuilder = new StringBuilder();
        return codeStream
                .doOnNext(contentBuilder::append)
                .doOnComplete(() -> {
                    // 流式返回后保存代码
                    try {
                        String completeCode = contentBuilder.toString();
                        if (completeCode.trim().isEmpty()) {
                            log.error("AI未生成任何代码内容，应用ID: {}", appId);
                            return;
                        }

                        log.debug("AI生成的原始内容: {}", completeCode);
                        Object parserResult = CodeParseExecutor.getParser(completeCode, type);
                        File file = CodeFileSaverExecutor.executeSaver(parserResult, type, appId);
                        log.info("代码保存成功：{}", file.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("文件保存失败，应用ID: {}, 错误信息: {}", appId, e.getMessage(), e);
                    }
                });
    }

}