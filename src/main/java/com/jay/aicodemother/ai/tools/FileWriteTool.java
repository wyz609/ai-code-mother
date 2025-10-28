package com.jay.aicodemother.ai.tools;

import com.jay.aicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
@Component
public class FileWriteTool {

    // 项目目录前缀常量
    private static final String PROJECT_DIR_PREFIX = "vue_project_";
    
    // 最大文件大小限制 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        try {
            // 参数验证
            if (relativeFilePath == null || relativeFilePath.trim().isEmpty()) {
                return "文件路径不能为空";
            }
            
            if (content == null) {
                content = ""; // 允许写入空文件
            }
            
            // 检查文件大小
            if (content.getBytes(StandardCharsets.UTF_8).length > MAX_FILE_SIZE) {
                return "文件内容过大，超过最大限制: " + MAX_FILE_SIZE + " 字节";
            }
            
            // 规范化路径
            Path path = Paths.get(relativeFilePath).normalize();
            
            // 安全检查：防止路径遍历攻击
            if (path.isAbsolute() || relativeFilePath.contains("..")) {
                return "不允许使用绝对路径或包含 .. 的路径";
            }
            
            // 相对路径处理，创建基于 appId 的项目目录
            String projectDirName = PROJECT_DIR_PREFIX + appId;
            Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
            path = projectRoot.resolve(path);
            
            // 再次规范化以防止路径遍历
            path = path.normalize();
            
            // 确保文件在项目根目录内
            if (!path.startsWith(projectRoot)) {
                return "文件路径超出允许范围";
            }
            
            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            
            // 写入文件内容
            Files.writeString(path, content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            
            log.info("成功写入文件: {}/{}", projectDirName, relativeFilePath);
            
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        } catch (Exception e) {
            String errorMessage = "文件写入发生未知错误: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}