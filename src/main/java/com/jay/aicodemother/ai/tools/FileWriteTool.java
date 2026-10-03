package com.jay.aicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool {

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
                    String relativeFilePath,
            @P("要写入文件的内容")
                    String content,
            @ToolMemoryId Long appId
    ) {
        try {
            // 安全解析项目内文件路径（防穿越/符号链接逃逸）
            Path path = resolveProjectPath(appId, relativeFilePath);
            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // 写入文件内容（不存在则创建，存在则整体覆盖）
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            // 记录写入成功日志
            log.info("成功写入文件: {}", path.toAbsolutePath());
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            // 写入失败构造错误信息返回给 AI
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        // 取文件相对路径参数
        String relativeFilePath = arguments.getStr("relativeFilePath");
        // 取写入内容参数
        String content = arguments.getStr("content");
        // 计算内容长度
        int contentLength = content == null ? 0 : content.length();
        // 文件内容已落盘，聊天流只保留操作摘要，避免把整份源码再次发送给前端。
        return String.format("[工具调用] %s %s（%d 个字符）",
                getDisplayName(), relativeFilePath, contentLength);
    }
}
