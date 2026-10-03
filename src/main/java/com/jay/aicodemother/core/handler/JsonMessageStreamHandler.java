package com.jay.aicodemother.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jay.aicodemother.ai.model.message.*;
import com.jay.aicodemother.ai.tools.BaseTool;
import com.jay.aicodemother.ai.tools.ToolManage;
import com.jay.aicodemother.constant.AppConstant;
import com.jay.aicodemother.core.builder.VueProjectBuilder;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.jay.aicodemother.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Class name: JsonMessageStreamHandler
 * Package: com.jay.aicodemother.core.handler
 * Description: JSON 消息流处理器 处理 VUE_PROJECT 类型的复杂流式响应， 包含工具调用相关信息
 *  该处理器是用来专门处理 VUE项目 (VUE_PROJECT类型) 生成代码时订单流失响应处理器， 它负者处理AI生成过程中不同类型的事件消息，包括AI响应
 *  工具调用请求和工具执行结果，并将这些信息适当的记录到对话历史中。
 *
 * @Create: 2025/10/25 21:40
 * @Author: jay
 * @Version: 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonMessageStreamHandler {

    private final VueProjectBuilder vueProjectBuilder;

    private final ToolManage toolManage;

    /**
     * 接收原始的JSON消息流
     * 处理每条消息并转换为前端可读的格式
     * 收集 AI 响应内容用于后续保存到历史
     * 在流完成或出错时更新对话历史
     * @param originFlux 原始的JSON消息流
     * @param chatHistoryService 对话历史服务
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 处理后的消息流
     */
    public Flux<String> handle(Flux<String> originFlux, ChatHistoryService chatHistoryService,
                               long appId, User loginUser){
        // 用于收集数据生成后端记忆格式 以便在流式完成保存到对话历史
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具 ID， 判断是否为第一次出现 避免重复显示工具调用信息
        Set<String> seenToolIds = new HashSet<>();
        // 对原始流做逐块转换：解析并处理每种消息类型
        return originFlux.mapNotNull(chunk -> {
            // 解析并处理单个 JSON 消息块，返回需要转发给前端的内容
            return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
        })
                // 过滤空内容（重复工具调用等返回空串的场景）
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    // 流式响应完成后， 添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    try {
                        // 将累积的 AI 响应保存为一条 AI 类型的历史消息
                        boolean success = chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                        // 保存失败仅记录日志
                        if (!success) {
                            log.error("保存AI响应到对话历史失败，appId: {}", appId);
                        }
                    } catch (Exception e) {
                        // 异常也仅记录日志
                        log.error("保存AI响应到对话历史时发生异常，appId: {}", appId, e);
                    }
                    // 异步构建 Vue 项目（npm install + npm run build）
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + System.getProperty("file.separator") + "vue_project_" + appId;
                    File projectDir = new File(projectPath);
                    // 只有项目目录完整且存在 package.json 才构建
                    if (projectDir.isDirectory() && new File(projectDir, "package.json").exists()) {
                        // 异步触发构建，不阻塞响应
                        vueProjectBuilder.buildProjectAsync(projectPath);
                    } else {
                        // 文件未生成完整时跳过构建
                        log.warn("Vue 项目文件尚未生成完整，跳过构建，appId: {}, 目录: {}", appId, projectPath);
                    }
                })
                .doOnError(error -> {
                    // 如果 AI 回复失败， 也需要记录错误信息
                    String errorMessage = " AI 回复失败：" + error.getMessage();
                    try {
                        // 将错误信息保存为一条 ERROR 类型的历史消息
                        boolean success = chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.ERROR.getValue(), loginUser.getId());
                        // 保存失败仅记录日志
                        if (!success) {
                            log.error("保存错误信息到对话历史失败，appId: {}", appId);
                        }
                    } catch (Exception e) {
                        // 异常仅记录日志
                        log.error("保存错误信息到对话历史时发生异常，appId: {}", appId, e);
                    }
                });
    }

    /**
     * 解析并处理单个 JSON 消息块，根据消息类型分发处理
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        // 将 JSON 字符串解析为流消息对象
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        // 根据类型枚举分发处理
        StreamMessageTypeEnum type = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (Objects.requireNonNull(type)){
            // AI 响应类型：直接转发给前端并累积到历史
            case  AI_RESPONSE -> {
                // 解析为 AI 响应消息
                AIResponseMessage aiMessage = JSONUtil.toBean(chunk, AIResponseMessage.class);
                String data = aiMessage.getData();
                // 拼接响应到历史缓冲
                chatHistoryStringBuilder.append(data);
                return data; // 将响应返回给前端
            }
            // 工具请求类型：生成工具调用提示，去重处理
            case TOOL_REQUEST -> {
                // 解析为工具请求消息
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                // 取工具调用 ID
                String toolId = toolRequestMessage.getId();
                // 取工具名称
                String toolName = toolRequestMessage.getName();
                // 检查是否为第一次调用这个工具 ID
                if(toolId != null && !seenToolIds.contains(toolId)){
                    // 是第一次调用该工具， 记录 ID 并完整的返回工具信息
                    seenToolIds.add(toolId);
                    // 根据工具名称获取工具实例
                    BaseTool tool = toolManage.getTool(toolName);
                    // 生成工具调用信息（前端展示用）
                    String result = tool.generateToolRequestResponse();
                    // 记录工具选择日志
                    log.info("[选择工具] 工具调用：{}", toolId);
                    return result;
                }else{
                    // 重复调用同一工具 ID 时忽略
                    log.info("[选择工具] 忽略重复工具调用：{}", toolId);
                    // 不是第一次调用该工具， 直接返回空
                    return "";
                }
            }
            // 工具执行完成类型：生成执行结果文本
            case TOOL_EXECUTED -> {
                // 解析为工具执行完成消息
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                // 取工具名称
                String toolName = toolExecutedMessage.getName();
                // 解析工具参数
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                // 根据工具名称获取到工具实例
                BaseTool tool = toolManage.getTool(toolName);
                // 根据工具实例获取到工具执行结果（含文件路径/成功/失败信息）
                String response = tool.generateToolExecutedResult(jsonObject);
//                String relativeFilePath = jsonObject.getStr("relativeFilePath");
//                String suffix = FileUtil.getSuffix(relativeFilePath);
//                String content = jsonObject.getStr("content");
//                String result = String.format("""
//                                [工具调用] 写入文件 %s
//                                ```%s
//                                %s
//                                ```
//                                """, relativeFilePath,suffix,content
//                );
                // 输出前端和要持久化的内容（前后加空行便于阅读）
                String output = String.format("\n\n%s\n\n", response);
                // 累积到历史缓冲
                chatHistoryStringBuilder.append(output);
                return output; // 返回给前端进行实时输出
            }
            // 未知消息类型
            default -> {
                log.error("不支持的消息类型： {}", type);
                return "";
            }
        }
    }

}