package com.jay.aicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 文件修改工具
 * 支持 AI 通过工具调用的方式修改文件内容
 */
@Slf4j
@Component
public class FileModifyTool extends BaseTool {

    @Tool("修改文件内容，用新内容替换指定的旧内容")
    public String modifyFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要替换的旧内容")
            String oldContent,
            @P("替换后的新内容")
            String newContent,
            @ToolMemoryId Long appId
    ) {
        try {
            // 安全解析项目内文件路径
            Path path = resolveProjectPath(appId, relativeFilePath);
            // 校验文件存在且为普通文件
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "错误：文件不存在或不是文件 - " + relativeFilePath;
            }
            // 读取文件原始内容
            String originalContent = Files.readString(path);
            // 旧内容必须能在文件中找到，否则无法替换
            if (!originalContent.contains(oldContent)) {
                return "警告：文件中未找到要替换的内容，文件未修改 - " + relativeFilePath;
            }
            // 执行内容替换
            String modifiedContent = originalContent.replace(oldContent, newContent);
            // 替换后无变化（新旧内容相同）则提示
            if (originalContent.equals(modifiedContent)) {
                return "信息：替换后文件内容未发生变化 - " + relativeFilePath;
            }
            // 写回文件（覆盖模式）
            Files.writeString(path, modifiedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            // 记录修改成功日志
            log.info("成功修改文件: {}", path.toAbsolutePath());
            // 返回相对路径的成功信息
            return "文件修改成功: " + relativeFilePath;
        } catch (IOException e) {
            // 修改失败构造错误信息
            String errorMessage = "修改文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getDisplayName() {
        return "修改文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        // 取文件相对路径
        String relativeFilePath = arguments.getStr("relativeFilePath");
        // 取被替换的旧内容
        String oldContent = arguments.getStr("oldContent");
        // 取替换后的新内容
        String newContent = arguments.getStr("newContent");
        // 计算新旧内容长度
        int oldContentLength = oldContent == null ? 0 : oldContent.length();
        int newContentLength = newContent == null ? 0 : newContent.length();
        // 原始和替换内容会很长；项目文件才是源码的唯一展示来源。
        return String.format("[工具调用] %s %s（替换 %d -> %d 个字符）",
                getDisplayName(), relativeFilePath, oldContentLength, newContentLength);
    }
}
