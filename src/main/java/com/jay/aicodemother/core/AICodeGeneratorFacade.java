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
import com.jay.aicodemother.ai.tools.BaseTool;
import com.jay.aicodemother.ai.tools.ToolManage;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
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

    @Resource
    private ToolManage toolManage;


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
        // 生成类型为空则报错
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        
        // 获取服务工厂实例
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = factory.getAiCodeGeneratorService(appId,codeGenTypeEnum);
        // 服务工厂未初始化时无法生成
        if (factory == null) {
            log.warn("AI代码生成服务工厂未初始化，无法生成代码");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI服务不可用");
        }
        // 按生成类型分发处理
        switch (codeGenTypeEnum) {
            // HTML 类型：生成单页面 HTML 代码并保存
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(appId, userMessage);
                CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            // 多文件类型：生成 HTML/CSS/JS 多文件并保存
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(appId, userMessage);
                CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            // 其他类型不支持
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
        // 生成类型为空则报错
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }

        // 服务工厂未初始化时返回错误信息流
        if (factory == null) {
            log.warn("AI代码生成服务工厂未初始化，无法生成代码");
            return Flux.just("错误：AI服务不可用");
        }
        // 判断是否为已存在的项目（增量修改模式）
        boolean existingProject = hasExistingProject(codeGenTypeEnum, appId);
        // 静态页面的首次生成由文本流直接保存，避免大段 HTML 作为工具 JSON 参数被模型错误转义。
        // 增量修改或 Vue 工程才启用文件工具
        boolean enableFileTools = existingProject || codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT;
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = factory.getAiCodeGeneratorService(appId, codeGenTypeEnum,
                enableFileTools);
        // 记录当前生成模式（首次生成/增量修改）
        log.info("AI 代码生成模式: {}, appId: {}, 类型: {}", existingProject ? "增量修改" : "首次生成", appId,
                codeGenTypeEnum.getValue());
        // 增量修改时给用户消息追加修改约束提示词
        String effectiveMessage = enhanceMessageForExistingProject(userMessage, codeGenTypeEnum, appId);
        // 按生成类型分发处理
        return switch (codeGenTypeEnum) {
            // HTML 类型
            case HTML -> {
                // 已有项目走增量修改流，否则走全新生成流
                Flux<String> codeStream = existingProject
                        ? processStaticProjectTokenStream(
                                aiCodeGeneratorService.modifyHtmlCodeStream(appId, effectiveMessage))
                        : aiCodeGeneratorService.generateHtmlCodeStream(appId, effectiveMessage);
                // 统一交给代码流处理器（负责落盘保存）
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId, existingProject, userMessage);
            }
            // 多文件类型
            case MULTI_FILE -> {
                // 已有项目走增量修改流，否则走全新生成流
                Flux<String> codeStream = existingProject
                        ? processStaticProjectTokenStream(
                                aiCodeGeneratorService.modifyMultiFileCodeStream(appId, effectiveMessage))
                        : aiCodeGeneratorService.generateMultiFileCodeStream(appId, effectiveMessage);
                // 统一交给代码流处理器
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId, existingProject, userMessage);
            }
            // Vue 工程类型
            case VUE_PROJECT -> {
                // 提前创建 Vue 项目根目录，保证文件工具在首次写入时能正确定位到 vue_project_<appId> 目录
                Path projectDir = projectPath(codeGenTypeEnum, appId);
                try {
                    // 递归创建项目目录
                    Files.createDirectories(projectDir);
                    log.info("已创建 Vue 项目目录: {}", projectDir);
                } catch (Exception e) {
                    // 目录创建失败则直接返回错误流
                    log.error("创建 Vue 项目目录失败: {}", projectDir, e);
                    yield Flux.error(e);
                }
                // Vue 项目的后续对话也必须携带增量修改约束，避免模型将其误判为新建工程。
                TokenStream codeStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, effectiveMessage);
                // Vue 工程走 TokenStream 处理器（含工具调用消息序列化）
                yield processTokenStream(codeStream);
            }
            // 其他类型不支持
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 增量修改已有项目时，在用户消息前追加项目约束提示词
     */
    private String enhanceMessageForExistingProject(String userMessage, CodeGenTypeEnum type, Long appId) {
        // 非已有项目直接返回原消息
        if (!hasExistingProject(type, appId)) {
            return userMessage;
        }
        // 已有项目：拼装「增量修改约束 + 用户修改要求」
        return existingProjectInstruction(type) + "\n\n用户修改要求：\n" + userMessage;
    }

    /**
     * 判断该项目是否已生成过代码（存在入口文件）
     */
    private boolean hasExistingProject(CodeGenTypeEnum type, Long appId) {
        // 计算项目根目录
        Path projectRoot = projectPath(type, appId);
        return switch (type) {
            // HTML/多文件：存在 index.html 即视为已生成
            case HTML, MULTI_FILE -> Files.isRegularFile(projectRoot.resolve("index.html"));
            // Vue 工程：package.json 是 Vue 工程已经生成完成的可靠标志；
            // 仅有一个预先创建的空目录时不应进入修改模式。
            case VUE_PROJECT -> Files.isRegularFile(projectRoot.resolve("package.json"))
                    && Files.isRegularFile(projectRoot.resolve("index.html"));
        };
    }

    /**
     * 生成不同类型项目的增量修改约束提示词（注入给 AI）
     */
    private String existingProjectInstruction(CodeGenTypeEnum type) {
        // 所有类型共用的修改规则
        String commonRules = """
                当前请求是对已生成项目的继续开发，不是重新创建项目。现有文件是唯一事实来源，必须保留用户此前已确认的功能、结构和样式。

                强制执行顺序：
                1. 先使用目录读取工具确认项目结构，再使用 readFile 读取每一个需要改动的文件；不得猜测文件内容。
                2. 已存在文件必须优先使用 modifyFile 做精确、最小范围的替换。只有需要新增的文件才可使用 writeFile。
                3. 禁止重写整个项目、禁止删除无关文件、禁止把完整项目源码输出到聊天回复中。
                4. 必须实际调用文件工具把修改写入磁盘；完成后仅用简短文字说明变更的文件和结果。
                5. 用户未要求的内容保持不变。若请求信息不足，先读取相关实现后作兼容性最好的局部调整。
                """;
        // 按类型追加专属约束
        return switch (type) {
            // 单 HTML 页面约束
            case HTML -> commonRules + """

                    当前项目为单 HTML 页面。先读取 index.html；只修改与本次需求相关的 HTML、CSS 或 JavaScript 片段，不得用 writeFile 重写整页。
                    """;
            // 多文件静态站约束
            case MULTI_FILE -> commonRules + """

                    当前项目为多文件静态网站。先读取目录结构，再读取需求关联的 HTML、CSS、JavaScript 文件；跨文件功能只修改必要文件，并保持现有文件引用关系有效。
                    """;
            // Vue 工程约束
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
        // 创建响应式流（sink 用于发射数据/错误/完成）
        return Flux.<String>create(sink -> {
            // 定义工具请求发射器：将工具请求序列化为 JSON 消息推给前端
            BiConsumer<Integer, ToolExecutionRequest> emitToolRequest = (index, toolExecutionRequest) -> {
                // 记录工具请求日志
                log.info("[工具请求] index: {}, id: {}, name: {}", index, toolExecutionRequest.id(),
                        toolExecutionRequest.name());
                // 构造工具请求消息对象
                ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                // 序列化并发射到流中
                sink.next(JSONUtil.toJsonStr(toolRequestMessage));
            };
            // 注册各回调：AI 分段响应
            codeStream.onPartialResponse((String partialResponse) ->{
                // 记录 AI 响应片段（调试级）
                log.debug("[AI响应] {}", partialResponse);
                // 构造 AI 响应消息并发射
                AIResponseMessage aiResponseMessage = new AIResponseMessage(partialResponse);
                sink.next(JSONUtil.toJsonStr(aiResponseMessage));
            })
                    // 工具请求发起与完成时都发射
                    .onPartialToolExecutionRequest(emitToolRequest)
                    .onCompleteToolExecutionRequest(emitToolRequest)
                    // 工具执行完成后发射执行结果
                    .onToolExecuted((ToolExecution toolExecution)->{
                        log.info("[工具执行完成] name: {}, result: {}", toolExecution.request().name(), toolExecution.result());
                        ToolExecutedMessage toolExecutionMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutionMessage));
                    })
                    // 完整响应完成时结束流
                    .onCompleteResponse((ChatResponse chatResponse) ->{
                        log.info("[流式响应完成]");
                        sink.complete();
                    })
                    // 出错时传播错误
                    .onError((Throwable error) ->{
                        log.error("[流式响应错误] {}", error.getMessage(), error);
                        sink.error(error);
                    })
                    // 启动 TokenStream
                    .start();
        // 添加背压处理：使用缓冲区并在溢出时丢弃最旧的数据
        }).onBackpressureBuffer(1000);
    }

    /**
     * 将静态项目的 Agent 工具事件转换为前端可直接展示的文本流。
     * 文件内容只展示本次写入或替换的片段，并限制长度，避免重新输出整个项目。
     */
    private Flux<String> processStaticProjectTokenStream(TokenStream codeStream) {
        // 创建响应式流
        return Flux.<String>create(sink -> {
            // 记录已宣布过的工具调用（去重，避免重复展示同一工具）
            Set<String> announcedTools = ConcurrentHashMap.newKeySet();
            // 工具宣布器：向流中输出 [工具调用] xxx 文本
            BiConsumer<Integer, ToolExecutionRequest> announceTool = (index, request) -> {
                // 取工具名
                String toolName = request.name();
                // 工具名为空则忽略
                if (toolName == null || toolName.isBlank()) {
                    return;
                }
                // 生成去重键：优先用工具请求 ID
                String requestKey = request.id() == null || request.id().isBlank()
                        ? index + ":" + toolName
                        : request.id();
                // 已宣布过的工具跳过
                if (!announcedTools.add(requestKey)) {
                    return;
                }
                // 查询工具元信息以获得可读名称
                BaseTool tool = toolManage.getTool(toolName);
                // 有展示名则用展示名，否则用原始工具名
                String displayName = tool == null ? toolName : tool.getDisplayName();
                // 输出工具调用提示
                sink.next(String.format("\n[工具调用] %s\n", displayName));
            };
            // 注册回调：分段响应直接转发
            codeStream.onPartialResponse(sink::next)
                    // 工具请求发起/完成时宣布
                    .onPartialToolExecutionRequest(announceTool)
                    .onCompleteToolExecutionRequest(announceTool)
                    // 工具执行完成时格式化输出
                    .onToolExecuted(toolExecution -> {
                        String output = formatStaticToolExecution(toolExecution);
                        if (!output.isBlank()) {
                            sink.next(output);
                        }
                    })
                    // 完成时结束流
                    .onCompleteResponse(response -> sink.complete())
                    // 出错时传播错误
                    .onError(sink::error)
                    .start();
        // 背压缓冲
        }).onBackpressureBuffer(1000);
    }

    /**
     * 格式化静态项目工具执行结果，输出给前端展示
     */
    private String formatStaticToolExecution(ToolExecution toolExecution) {
        // 取工具名
        String toolName = toolExecution.request().name();
        // 取工具参数 JSON 字符串
        String arguments = toolExecution.request().arguments();
        cn.hutool.json.JSONObject argumentObject;
        try {
            // 解析参数 JSON
            argumentObject = JSONUtil.parseObj(arguments);
        } catch (Exception e) {
            // 解析失败仅输出工具名
            log.warn("无法解析工具展示参数，工具: {}", toolName, e);
            return String.format("\n✓ %s\n", toolName);
        }

        // 取相对文件路径（缺省用工具名）
        String relativeFilePath = argumentObject.getStr("relativeFilePath", toolName);
        // 取工具执行结果
        String executionResult = toolExecution.result();
        // 结果以"错误/警告/失败"开头时输出失败提示
        if (executionResult != null && (executionResult.startsWith("错误")
                || executionResult.startsWith("警告")
                || executionResult.contains("失败"))) {
            return String.format("\n[工具失败] %s\n%s\n", relativeFilePath, executionResult);
        }
        // 正常情况：输出成功标记 + 文件路径
        StringBuilder output = new StringBuilder("\n✓ ").append(relativeFilePath).append('\n');
        // 提取本次变更的内容：modifyFile 取 newContent，writeFile 取 content
        String changedContent = switch (toolName) {
            case "modifyFile" -> argumentObject.getStr("newContent");
            case "writeFile" -> argumentObject.getStr("content");
            default -> null;
        };
        // 无变更内容则只输出成功标记
        if (changedContent == null || changedContent.isBlank()) {
            return output.toString();
        }

        // 限制预览长度，避免一次性输出整个项目
        int previewLimit = 2400;
        // 是否被截断
        boolean truncated = changedContent.length() > previewLimit;
        // 截断到预览上限
        String preview = truncated ? changedContent.substring(0, previewLimit) : changedContent;
        // 根据文件扩展名推导代码语言
        String language = fileLanguage(relativeFilePath);
        // 拼装代码块：语言 + 内容
        output.append("\n修改内容：\n```")
                .append(language)
                .append('\n')
                .append(preview)
                .append('\n');
        // 被截断时提示完整内容在查看代码中打开
        if (truncated) {
            output.append("/* 内容较长，仅展示前 2400 个字符；完整内容请在查看代码中打开 */\n");
        }
        // 关闭代码块
        return output.append("```\n").toString();
    }

    /**
     * 根据文件路径推导代码语言（用于代码块高亮）
     */
    private String fileLanguage(String relativeFilePath) {
        // 路径为空返回 text
        if (relativeFilePath == null) {
            return "text";
        }
        // 取扩展名索引
        int extensionIndex = relativeFilePath.lastIndexOf('.');
        // 无扩展名返回 text
        if (extensionIndex < 0 || extensionIndex == relativeFilePath.length() - 1) {
            return "text";
        }
        // 按扩展名映射语言
        return switch (relativeFilePath.substring(extensionIndex + 1).toLowerCase(Locale.ROOT)) {
            case "js" -> "javascript";
            case "ts" -> "typescript";
            default -> relativeFilePath.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        };
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
        // 增量模式下记录修改前的 index.html 内容（用于判断工具是否真正落盘）
        String originalContent = existingProject ? readIndexHtml(type, appId) : null;
        return codeStream
                // 每收到一段内容就累积到 builder
                .doOnNext(contentBuilder::append)
                // 流完成时执行保存逻辑
                .doOnComplete(() -> {
                    // 增量编辑已经通过文件工具落盘，不能把简短说明再次当成完整代码保存。
                    try {
                        // 取出累积的完整内容
                        String completeCode = contentBuilder.toString();
                        // 增量修改模式
                        if (existingProject) {
                            // 重新读取当前 index.html
                            String currentContent = readIndexHtml(type, appId);
                            // 内容与修改前一致说明工具未真正落盘，走兜底规则
                            if (originalContent != null && originalContent.equals(currentContent)) {
                                applyCommonHtmlEditFallback(type, appId, userMessage);
                            }
                            // 增量模式跳过文本覆盖保存
                            log.info("检测到已有项目，跳过 AI 文本覆盖保存，appId: {}", appId);
                            return;
                        }
                        // 首次生成：检查内容是否为完整 HTML 文档
                        if (!containsCompleteHtmlDocument(completeCode, type)) {
                            // 内容不完整时尝试从工具写入的文件中提升入口文件
                            if (promoteGeneratedHtmlEntryFile(type, appId)) {
                                log.info("检测到文件工具已写入 HTML，跳过文本保存，appId: {}", appId);
                            } else {
                                log.warn("AI 未返回可保存的完整代码，且未找到工具生成的 HTML 文件，appId: {}", appId);
                            }
                            return;
                        }

                        // 记录 AI 生成的原始内容（调试级）
                        log.debug("AI生成的原始内容: {}", completeCode);
                        // 根据类型解析代码结果
                        Object parserResult = CodeParseExecutor.getParser(completeCode, type);
                        // 保存解析结果到文件系统
                        File file = CodeFileSaverExecutor.executeSaver(parserResult, type, appId);
                        // 记录保存成功路径
                        log.info("代码保存成功：{}", file.getAbsolutePath());
                    } catch (Exception e) {
                        // 保存失败记录日志并抛出异常
                        log.error("文件保存失败，应用ID: {}, 错误信息: {}", appId, e.getMessage(), e);
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成代码未能保存，请重新生成");
                    }
                });
    }

    /**
     * 判断内容是否为完整的 HTML 文档
     */
    private boolean containsCompleteHtmlDocument(String content, CodeGenTypeEnum type) {
        // 非 HTML 类型：内容非空即可
        if (type != CodeGenTypeEnum.HTML || content == null) {
            return type != CodeGenTypeEnum.HTML && content != null && !content.isBlank();
        }
        // HTML 类型：必须同时包含 <html 与 </html>
        String normalized = content.toLowerCase(Locale.ROOT);
        return normalized.contains("<html") && normalized.contains("</html>");
    }

    /**
     * HTML 预览固定读取 index.html。模型通过 writeFile 首次生成时可能使用了语义化文件名，
     * 因此仅在项目根目录中恰好存在一个 HTML 文件时将其复制为入口文件。
     */
    private boolean promoteGeneratedHtmlEntryFile(CodeGenTypeEnum type, Long appId) {
        // 仅 HTML 类型适用
        if (type != CodeGenTypeEnum.HTML) {
            return false;
        }
        // 计算项目目录
        Path projectDir = projectPath(type, appId);
        // 入口文件路径
        Path indexFile = projectDir.resolve("index.html");
        try {
            // index.html 已存在且有内容则直接可用
            if (Files.isRegularFile(indexFile) && Files.size(indexFile) > 0) {
                return true;
            }
            // 遍历项目根目录找第一个非空 HTML 文件
            try (Stream<Path> files = Files.list(projectDir)) {
                Path generatedHtml = files
                        .filter(Files::isRegularFile)
                        // 扩展名必须是 .html
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".html"))
                        // 文件必须非空
                        .filter(path -> {
                            try {
                                return Files.size(path) > 0;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        // 取第一个
                        .findFirst()
                        .orElse(null);
                // 没有可用的 HTML 文件则失败
                if (generatedHtml == null) {
                    return false;
                }
                // 将该文件复制为 index.html 作为预览入口
                Files.copy(generatedHtml, indexFile);
                // 记录入口提升日志
                log.info("已将工具生成的 HTML 文件设为预览入口: {} -> {}",
                        generatedHtml.getFileName(), indexFile.getFileName());
                return true;
            }
        } catch (Exception e) {
            // 异常仅记录日志
            log.warn("设置 HTML 预览入口失败，appId: {}", appId, e);
            return false;
        }
    }

    /**
     * 读取项目入口 index.html 内容
     */
    private String readIndexHtml(CodeGenTypeEnum type, Long appId) {
        // 计算入口文件路径
        Path file = projectPath(type, appId).resolve("index.html");
        try {
            // 文件存在则读取全文，否则返回 null
            return Files.isRegularFile(file) ? Files.readString(file) : null;
        } catch (Exception e) {
            // 读取失败记录日志
            log.warn("读取项目文件失败，appId: {}", appId, e);
            return null;
        }
    }

    /**
     * 计算项目根目录路径：{user.dir}/tmp/code_output/{类型}_{appId}
     */
    private Path projectPath(CodeGenTypeEnum type, Long appId) {
        return Path.of(System.getProperty("user.dir"), "tmp", "code_output",
                type.getValue() + "_" + appId);
    }

    /**
     * 工具没有真正执行时，处理最明确且无歧义的常见样式请求，避免只返回口头确认。
     */
    private void applyCommonHtmlEditFallback(CodeGenTypeEnum type, Long appId, String userMessage) {
        // 仅 HTML 类型且消息非空才处理
        if (type != CodeGenTypeEnum.HTML || userMessage == null) return;
        // 消息转小写便于匹配
        String request = userMessage.toLowerCase(Locale.ROOT);
        // 仅当用户请求包含"背景"和"黑色"时才触发兜底
        if (!(request.contains("背景") && (request.contains("黑色") || request.contains("黑")))) return;

        // 计算入口文件路径
        Path file = projectPath(type, appId).resolve("index.html");
        try {
            // 读取当前文件内容
            String content = Files.readString(file);
            // 尝试替换背景渐变为纯黑色（兼容有无空格两种写法）
            String updated = content
                    .replace("background: var(--bg-gradient);", "background: #000000;")
                    .replace("background:var(--bg-gradient);", "background:#000000;");
            // 内容有变化则写入
            if (!content.equals(updated)) {
                Files.writeString(file, updated, StandardOpenOption.TRUNCATE_EXISTING);
                log.info("AI 未执行文件工具，已通过兜底规则修改背景为黑色，文件: {}", file);
            } else {
                // 未匹配到目标样式则记录警告
                log.warn("未找到可匹配的背景样式，未执行兜底修改，文件: {}", file);
            }
        } catch (Exception e) {
            // 兜底修改失败记录日志
            log.error("执行背景修改兜底失败，appId: {}", appId, e);
        }
    }

}
