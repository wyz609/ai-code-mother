package com.jay.aicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import org.springframework.stereotype.Component;

import com.jay.aicodemother.constant.AppConstant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 工具基类
 * 定义所有工具的通用接口
 */
//@Component
public abstract class BaseTool {

    /** Resolves a tool path while preventing absolute paths, traversal and symlink escapes. */
    protected Path resolveProjectPath(Long appId, String userPath) throws IOException {
        if (appId == null || appId <= 0) {
            throw new IOException("应用 ID 无效");
        }
        if (userPath == null || userPath.isBlank()) {
            throw new IOException("文件路径不能为空");
        }

        Path outputRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
        Path projectRoot = findProjectRoot(outputRoot, appId);
        Path requested = Paths.get(userPath);
        if (requested.isAbsolute()) {
            throw new IOException("只允许使用项目内相对路径");
        }

        Path candidate = projectRoot.resolve(requested).normalize();
        Path rootReal = realPathOrAbsolute(projectRoot);
        if (!candidate.startsWith(projectRoot)) {
            throw new IOException("路径超出项目目录");
        }

        // Resolve the existing portion to catch symlinks, while still allowing a new file to be created.
        Deque<String> missingParts = new ArrayDeque<>();
        Path existing = candidate;
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            Path fileName = existing.getFileName();
            if (fileName == null) {
                break;
            }
            missingParts.push(fileName.toString());
            existing = existing.getParent();
        }
        Path resolved = realPathOrAbsolute(existing);
        while (!missingParts.isEmpty()) {
            resolved = resolved.resolve(missingParts.pop());
        }
        resolved = resolved.normalize();
        if (!resolved.startsWith(rootReal)) {
            throw new IOException("路径超出项目目录");
        }
        return resolved;
    }

    private Path findProjectRoot(Path outputRoot, Long appId) throws IOException {
        String[] typePrefixes = {"html_", "multi_file_", "vue_project_"};
        for (String prefix : typePrefixes) {
            Path candidate = outputRoot.resolve(prefix + appId).normalize();
            if (Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)) {
                return candidate;
            }
        }
        // 首次生成时，HTML 是默认类型；工具可以负责创建该目录。
        return outputRoot.resolve("html_" + appId).normalize();
    }

    private Path realPathOrAbsolute(Path path) throws IOException {
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                ? path.toRealPath()
                : path.toAbsolutePath().normalize();
    }

    /**
     * 获取工具的英文名称（对应方法名）
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具的中文显示名称
     *
     * @return 工具中文名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求时的返回值（显示给用户）
     *
     * @return 工具请求显示内容
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成工具执行结果格式（保存到数据库）
     *
     * @param arguments 工具执行参数
     * @return 格式化的工具执行结果
     */
    public abstract String generateToolExecutedResult(JSONObject arguments);
}
