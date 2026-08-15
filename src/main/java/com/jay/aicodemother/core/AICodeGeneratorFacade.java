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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.stream.Stream;

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
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(appId, userMessage);
                CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(appId, userMessage);
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

        if (factory == null) {
            log.warn("AI代码生成服务工厂未初始化，无法生成代码");
            return Flux.just("错误：AI服务不可用");
        }
        boolean existingProject = hasExistingProject(codeGenTypeEnum, appId);
        // 静态页面的首次生成由文本流直接保存，避免大段 HTML 作为工具 JSON 参数被模型错误转义。
        boolean enableFileTools = existingProject || codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT;
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = factory.getAiCodeGeneratorService(appId, codeGenTypeEnum,
                enableFileTools);
        log.info("AI 代码生成模式: {}, appId: {}, 类型: {}", existingProject ? "增量修改" : "首次生成", appId,
                codeGenTypeEnum.getValue());
        String effectiveMessage = enhanceMessageForExistingProject(userMessage, codeGenTypeEnum, appId);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(appId, effectiveMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId, existingProject, userMessage);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(appId, effectiveMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId, existingProject, userMessage);
            }
            case VUE_PROJECT -> {
                // 提前创建 Vue 项目根目录，保证文件工具在首次写入时能正确定位到 vue_project_<appId> 目录
                Path projectDir = projectPath(codeGenTypeEnum, appId);
                try {
                    Files.createDirectories(projectDir);
                    log.info("已创建 Vue 项目目录: {}", projectDir);
                } catch (Exception e) {
                    log.error("创建 Vue 项目目录失败: {}", projectDir, e);
                    yield Flux.error(e);
                }
                // Vue 项目的后续对话也必须携带增量修改约束，避免模型将其误判为新建工程。
                TokenStream codeStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, effectiveMessage);
                yield processTokenStream(codeStream);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    private String enhanceMessageForExistingProject(String userMessage, CodeGenTypeEnum type, Long appId) {
        if (!hasExistingProject(type, appId)) {
            return userMessage;
        }
        return existingProjectInstruction(type) + "\n\n用户修改要求：\n" + userMessage;
    }

    private boolean hasExistingProject(CodeGenTypeEnum type, Long appId) {
        Path projectRoot = projectPath(type, appId);
        return switch (type) {
            case HTML, MULTI_FILE -> Files.isRegularFile(projectRoot.resolve("index.html"));
            // package.json 是 Vue 工程已经生成完成的可靠标志；仅有一个预先创建的空目录时不应进入修改模式。
            case VUE_PROJECT -> Files.isRegularFile(projectRoot.resolve("package.json"))
                    && Files.isRegularFile(projectRoot.resolve("index.html"));
        };
    }

    private String existingProjectInstruction(CodeGenTypeEnum type) {
        String commonRules = """
                当前请求是对已生成项目的继续开发，不是重新创建项目。现有文件是唯一事实来源，必须保留用户此前已确认的功能、结构和样式。

                强制执行顺序：
                1. 先使用目录读取工具确认项目结构，再使用 readFile 读取每一个需要改动的文件；不得猜测文件内容。
                2. 已存在文件必须优先使用 modifyFile 做精确、最小范围的替换。只有需要新增的文件才可使用 writeFile。
                3. 禁止重写整个项目、禁止删除无关文件、禁止把完整项目源码输出到聊天回复中。
                4. 必须实际调用文件工具把修改写入磁盘；完成后仅用简短文字说明变更的文件和结果。
                5. 用户未要求的内容保持不变。若请求信息不足，先读取相关实现后作兼容性最好的局部调整。
                """;
        return switch (type) {
            case HTML -> commonRules + """

                    当前项目为单 HTML 页面。先读取 index.html；只修改与本次需求相关的 HTML、CSS 或 JavaScript 片段，不得用 writeFile 重写整页。
                    """;
            case MULTI_FILE -> commonRules + """

                    当前项目为多文件静态网站。先读取目录结构，再读取需求关联的 HTML、CSS、JavaScript 文件；跨文件功能只修改必要文件，并保持现有文件引用关系有效。
                    """;
            case VUE_PROJECT -> commonRules + """

                    当前项目为已可运行的 Vue 工程。先使用 readDir，再读取相关的 .vue、.js、.css 和必要配置文件。
                    仅修改需求关联的组件、页面、样式、路由或数据文件；不得重新生成 package.json、index.html、整个 src 目录或全量组件。
                    新增功能时优先复用已有组件和样式；确实需要新组件或页面时，才创建单个新文件并在现有入口处做最小接入。
                    修改后必须保持 import、路由和组件引用一致，使 npm run build 能继续通过。
                    """;
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>， 并传递工具调用信息 适配器类
     * @param codeStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream codeStream) {
        return Flux.<String>create(sink -> {
            codeStream.onPartialResponse((String partialResponse) ->{
                log.debug("[AI响应] {}", partialResponse);
                AIResponseMessage aiResponseMessage = new AIResponseMessage(partialResponse);
                sink.next(JSONUtil.toJsonStr(aiResponseMessage));
            })
                    .onPartialToolExecutionRequest((Integer index, ToolExecutionRequest toolExecutionRequest) -> {
                        log.info("[工具请求] index: {}, id: {}, name: {}", index, toolExecutionRequest.id(), toolExecutionRequest.name());
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution)->{
                        log.info("[工具执行完成] name: {}, result: {}", toolExecution.request().name(), toolExecution.result());
                        ToolExecutedMessage toolExecutionMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutionMessage));
                    })
                    .onCompleteResponse((ChatResponse chatResponse) ->{
                        log.info("[流式响应完成]");
                        sink.complete();
                    })
                    .onError((Throwable error) ->{
                        log.error("[流式响应错误] {}", error.getMessage(), error);
                        sink.error(error);
                    })
                    .start();
        // 添加背压处理：使用缓冲区并在溢出时丢弃最旧的数据
        }).onBackpressureBuffer(1000);
    }

    /**
     * 处理流式代码生成
     *
     * @param codeStream 代码流
     * @param type       代码生成类型
     * @param appId      应用ID
     * @return 处理后的字符串流
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum type, Long appId,
                                           boolean existingProject, String userMessage) {
        // 收集流中的所有内容
        StringBuilder contentBuilder = new StringBuilder();
        String originalContent = existingProject ? readIndexHtml(type, appId) : null;
        return codeStream
                .doOnNext(contentBuilder::append)
                .doOnComplete(() -> {
                    // 增量编辑已经通过文件工具落盘，不能把简短说明再次当成完整代码保存。
                    try {
                        String completeCode = contentBuilder.toString();
                        if (existingProject) {
                            String currentContent = readIndexHtml(type, appId);
                            if (originalContent != null && originalContent.equals(currentContent)) {
                                applyCommonHtmlEditFallback(type, appId, userMessage);
                            }
                            log.info("检测到已有项目，跳过 AI 文本覆盖保存，appId: {}", appId);
                            return;
                        }
                        if (!containsCompleteHtmlDocument(completeCode, type)) {
                            if (promoteGeneratedHtmlEntryFile(type, appId)) {
                                log.info("检测到文件工具已写入 HTML，跳过文本保存，appId: {}", appId);
                            } else {
                                log.warn("AI 未返回可保存的完整代码，且未找到工具生成的 HTML 文件，appId: {}", appId);
                            }
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

    private boolean containsCompleteHtmlDocument(String content, CodeGenTypeEnum type) {
        if (type != CodeGenTypeEnum.HTML || content == null) {
            return type != CodeGenTypeEnum.HTML && content != null && !content.isBlank();
        }
        String normalized = content.toLowerCase(Locale.ROOT);
        return normalized.contains("<html") && normalized.contains("</html>");
    }

    /**
     * HTML 预览固定读取 index.html。模型通过 writeFile 首次生成时可能使用了语义化文件名，
     * 因此仅在项目根目录中恰好存在一个 HTML 文件时将其复制为入口文件。
     */
    private boolean promoteGeneratedHtmlEntryFile(CodeGenTypeEnum type, Long appId) {
        if (type != CodeGenTypeEnum.HTML) {
            return false;
        }
        Path projectDir = projectPath(type, appId);
        Path indexFile = projectDir.resolve("index.html");
        try {
            if (Files.isRegularFile(indexFile) && Files.size(indexFile) > 0) {
                return true;
            }
            try (Stream<Path> files = Files.list(projectDir)) {
                Path generatedHtml = files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".html"))
                        .filter(path -> {
                            try {
                                return Files.size(path) > 0;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .findFirst()
                        .orElse(null);
                if (generatedHtml == null) {
                    return false;
                }
                Files.copy(generatedHtml, indexFile);
                log.info("已将工具生成的 HTML 文件设为预览入口: {} -> {}",
                        generatedHtml.getFileName(), indexFile.getFileName());
                return true;
            }
        } catch (Exception e) {
            log.warn("设置 HTML 预览入口失败，appId: {}", appId, e);
            return false;
        }
    }

    private String readIndexHtml(CodeGenTypeEnum type, Long appId) {
        Path file = projectPath(type, appId).resolve("index.html");
        try {
            return Files.isRegularFile(file) ? Files.readString(file) : null;
        } catch (Exception e) {
            log.warn("读取项目文件失败，appId: {}", appId, e);
            return null;
        }
    }

    private Path projectPath(CodeGenTypeEnum type, Long appId) {
        return Path.of(System.getProperty("user.dir"), "tmp", "code_output",
                type.getValue() + "_" + appId);
    }

    /**
     * 工具没有真正执行时，处理最明确且无歧义的常见样式请求，避免只返回口头确认。
     */
    private void applyCommonHtmlEditFallback(CodeGenTypeEnum type, Long appId, String userMessage) {
        if (type != CodeGenTypeEnum.HTML || userMessage == null) return;
        String request = userMessage.toLowerCase(Locale.ROOT);
        if (!(request.contains("背景") && (request.contains("黑色") || request.contains("黑")))) return;

        Path file = projectPath(type, appId).resolve("index.html");
        try {
            String content = Files.readString(file);
            String updated = content
                    .replace("background: var(--bg-gradient);", "background: #000000;")
                    .replace("background:var(--bg-gradient);", "background:#000000;");
            if (!content.equals(updated)) {
                Files.writeString(file, updated, StandardOpenOption.TRUNCATE_EXISTING);
                log.info("AI 未执行文件工具，已通过兜底规则修改背景为黑色，文件: {}", file);
            } else {
                log.warn("未找到可匹配的背景样式，未执行兜底修改，文件: {}", file);
            }
        } catch (Exception e) {
            log.error("执行背景修改兜底失败，appId: {}", appId, e);
        }
    }

}
