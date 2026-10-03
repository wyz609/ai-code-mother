package com.jay.aicodemother.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 文件目录读取工具
 * 使用 Hutool 简化文件操作
 */
@Slf4j
@Component
public class FileDirReadTool extends BaseTool{

    private static final int MAX_ENTRIES = 500;

    /**
     * 需要忽略的文件和目录
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    /**
     * 需要忽略的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );

    @Tool("读取目录结构，获取指定目录下的所有文件和子目录信息")
    public String readDir(
            @P("目录的相对路径，为空则读取整个项目结构")
            String relativeDirPath,
            @ToolMemoryId Long appId
    ) {
        // 目录路径为空时默认读取项目根目录（. 表示当前目录）
        String safeRelativeDirPath = StrUtil.isBlank(relativeDirPath) ? "." : relativeDirPath.trim();
        try {
            // 安全解析项目内目录路径
            Path path = resolveProjectPath(appId, safeRelativeDirPath);
            // 校验目录存在且为目录
            if (!Files.isDirectory(path)) {
                return "错误：目录不存在或不是目录 - " + safeRelativeDirPath;
            }
            // 收集目录下所有条目（文件与子目录）
            List<DirectoryEntry> entries = collectEntries(path);
            // 构建目录结构字符串
            StringBuilder structure = new StringBuilder();
            structure.append("项目目录结构:\n");
            // 按相对路径排序，限制展示数量，逐行输出树形结构
            entries.stream()
                    .sorted(Comparator.comparing(entry -> entry.relativePath().toString()))
                    .limit(MAX_ENTRIES)
                    .forEach(entry -> {
                        // 计算缩进层级（相对路径段数 - 1）
                        int depth = Math.max(0, entry.relativePath().getNameCount() - 1);
                        // 输出缩进
                        structure.append("  ".repeat(depth))
                                // 输出条目名
                                .append(entry.relativePath().getFileName());
                        // 目录追加 / 后缀便于识别
                        if (entry.directory()) {
                            structure.append('/');
                        }
                        structure.append('\n');
                    });
            // 条目过多时提示省略数量
            if (entries.size() > MAX_ENTRIES) {
                structure.append("... 已省略 ")
                        .append(entries.size() - MAX_ENTRIES)
                        .append(" 个条目\n");
            }
            // 返回目录结构文本
            return structure.toString();

        } catch (Exception e) {
            // 读取失败构造错误信息
            String errorMessage = "读取目录结构失败: " + safeRelativeDirPath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 递归收集目录下所有文件与子目录条目（忽略指定目录）
     */
    private List<DirectoryEntry> collectEntries(Path root) throws IOException {
        // 收集结果列表
        List<DirectoryEntry> entries = new ArrayList<>();
        // 递归遍历目录树
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // 忽略目录本身（root 不加入列表）
                // 命中忽略目录时跳过整个子树（不递归进入）
                if (!dir.equals(root) && shouldIgnore(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                // 子目录加入条目列表
                if (!dir.equals(root)) {
                    entries.add(new DirectoryEntry(root.relativize(dir), true));
                }
                // 继续遍历
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // 非忽略文件才加入条目
                if (!shouldIgnore(file.getFileName().toString())) {
                    entries.add(new DirectoryEntry(root.relativize(file), false));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        // 返回所有条目
        return entries;
    }

    /**
     * 判断是否应该忽略该文件或目录
     */
    private boolean shouldIgnore(String fileName) {
        // 检查是否在忽略名称列表中（node_modules/.git/dist 等）
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }

        // 检查文件扩展名（.log/.tmp/.cache 等）
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    // 目录条目记录：相对路径 + 是否为目录
    private record DirectoryEntry(Path relativePath, boolean directory) {
    }

    @Override
    public String getToolName() {
        return "readDir";
    }

    @Override
    public String getDisplayName() {
        return "读取目录";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        // 取目录相对路径
        String relativeDirPath = arguments.getStr("relativeDirPath");
        // 为空则显示根目录
        if (StrUtil.isEmpty(relativeDirPath)) {
            relativeDirPath = "根目录";
        }
        // 返回读取操作摘要
        return String.format("[工具调用] %s %s", getDisplayName(), relativeDirPath);
    }
}
