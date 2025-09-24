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

import com.jay.aicodemother.ai.AiCodeGeneratorService;
import com.jay.aicodemother.ai.model.HtmlCodeResult;
import com.jay.aicodemother.ai.model.MultiFileCodeResult;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import com.jay.aicodemother.parser.CodeParseExecutor;
import com.jay.aicodemother.save.CodeFileSaverExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;

/**
 * AI代码生成门面类
 * 提供统一的接口来生成不同类型的代码并保存到文件系统中
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AICodeGeneratorFacade {
    
    /**
     * AI代码生成服务，通过构造函数注入
     */
    private final AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId 应用ID
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE,appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
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
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }
    
    /**
     * 处理流式代码生成
     *
     * @param codeStream 代码流
     * @param type 代码生成类型
     * @param appId 应用ID
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
                        Object parserResult = CodeParseExecutor.getParser(completeCode, type);
                        File file = CodeFileSaverExecutor.executeSaver(parserResult, type, appId);
                        log.info("代码保存成功：{}", file.getAbsolutePath());
                    }catch (Exception e){
                        log.error("文件保存失败,{}",e.getMessage());
                    }
                });
    }
}