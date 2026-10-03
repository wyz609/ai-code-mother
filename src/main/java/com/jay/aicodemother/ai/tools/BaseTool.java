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
        // 校验应用 ID 有效
        if (appId == null || appId <= 0) {
            throw new IOException("应用 ID 无效");
        }
        // 校验文件路径非空
        if (userPath == null || userPath.isBlank()) {
            throw new IOException("文件路径不能为空");
        }

        // 代码输出根目录（绝对路径规范化）
        Path outputRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
        // 找到该应用对应的项目根目录
        Path projectRoot = findProjectRoot(outputRoot, appId);
        // 解析用户传入的相对路径
        Path requested = Paths.get(userPath);
        // 拒绝绝对路径（只允许项目内相对路径）
        if (requested.isAbsolute()) {
            throw new IOException("只允许使用项目内相对路径");
        }

        // 拼接出候选文件路径并规范化（消除 .. 等）
        Path candidate = projectRoot.resolve(requested).normalize();
        // 解析项目根目录的真实路径
        Path rootReal = realPathOrAbsolute(projectRoot);
        // 候选路径必须位于项目根目录内（防目录穿越）
        if (!candidate.startsWith(projectRoot)) {
            throw new IOException("路径超出项目目录");
        }

        // Resolve the existing portion to catch symlinks, while still allowing a new file to be created.
        // 记录路径中不存在的部分（用于新建文件场景）
        Deque<String> missingParts = new ArrayDeque<>();
        // 从候选路径向上找第一个已存在的路径段
        Path existing = candidate;
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            // 取出当前段文件名
            Path fileName = existing.getFileName();
            // 已到根目录则停止
            if (fileName == null) {
                break;
            }
            // 将缺失段压栈
            missingParts.push(fileName.toString());
            // 继续向父目录查找
            existing = existing.getParent();
        }
        // 解析已存在部分的真实路径（捕获符号链接）
        Path resolved = realPathOrAbsolute(existing);
        // 把缺失段重新拼接回路径
        while (!missingParts.isEmpty()) {
            resolved = resolved.resolve(missingParts.pop());
        }
        // 再次规范化
        resolved = resolved.normalize();
        // 最终安全校验：解析后的路径必须在项目根目录内
        if (!resolved.startsWith(rootReal)) {
            throw new IOException("路径超出项目目录");
        }
        // 返回安全解析后的完整路径
        return resolved;
    }

    /**
     * 在代码输出根目录下查找指定应用的项目根目录。
     * 按 html_/multi_file_/vue_project_ 三种前缀依次查找。
     */
    private Path findProjectRoot(Path outputRoot, Long appId) throws IOException {
        // 三种可能的目录前缀
        String[] typePrefixes = {"html_", "multi_file_", "vue_project_"};
        // 依次检查哪种类型的项目目录已存在
        for (String prefix : typePrefixes) {
            Path candidate = outputRoot.resolve(prefix + appId).normalize();
            // 命中已存在目录则返回
            if (Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)) {
                return candidate;
            }
        }
        // 首次生成时，HTML 是默认类型；工具可以负责创建该目录。
        return outputRoot.resolve("html_" + appId).normalize();
    }

    /**
     * 返回路径的真实路径（若存在），否则返回绝对规范化路径。
     * 用于捕获符号链接、同时支持新建文件的路径解析。
     */
    private Path realPathOrAbsolute(Path path) throws IOException {
        // 存在则解析真实路径，否则返回绝对路径
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
