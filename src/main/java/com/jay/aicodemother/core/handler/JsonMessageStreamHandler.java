package com.jay.aicodemother.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jay.aicodemother.ai.model.message.*;
import com.jay.aicodemother.constant.AppConstant;
import com.jay.aicodemother.core.builder.VueProjectBuilder;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.jay.aicodemother.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

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
        return originFlux.mapNotNull(chunk -> {
            // 解析每个 JSON 消息块
            return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
        })
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    // 流式响应完成后， 添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    // 异步构建 Vue 项目
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                    vueProjectBuilder.buildProjectAsync(projectPath);
                })
                .doOnError(error -> {
                    // 如果 AI 回复失败， 也需要记录错误信息
                    String errorMessage = " AI 回复失败：" + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.ERROR.getValue(), loginUser.getId());
                });
    }

    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {

        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum type = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (Objects.requireNonNull(type)){
            case  AI_RESPONSE -> {
                AIResponseMessage aiMessage = JSONUtil.toBean(chunk, AIResponseMessage.class);
                String data = aiMessage.getData();
                // 拼接响应
                chatHistoryStringBuilder.append(data);
                return data; // 将响应返回给前端
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                // 检查是否为第一次调用这个工具 ID
                if(toolId != null && !seenToolIds.contains(toolId)){
                    // 是第一次调用该工具， 记录 ID 并完整的返回工具信息
                    seenToolIds.add(toolId);
                    log.info("[选择工具] 工具调用：{}", toolId);
                    return "\n\n[选择工具] 写入文件\n\n";
                }else{
                    log.info("[选择工具] 忽略重复工具调用：{}", toolId);
                    // 不是第一次调用该工具， 直接返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                String relativeFilePath = jsonObject.getStr("relativeFilePath");
                String suffix = FileUtil.getSuffix(relativeFilePath);
                String content = jsonObject.getStr("content");
                String result = String.format("""
                                [工具调用] 写入文件 %s
                                ```%s
                                %s
                                ```
                                """, relativeFilePath,suffix,content
                );
                // 输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output; // 返回给前端进行实时输出
            }
            default -> {
                log.error("不支持的消息类型： {}", type);
                return "";
            }
        }
    }

}