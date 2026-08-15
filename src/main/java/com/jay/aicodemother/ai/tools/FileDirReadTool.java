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
        String safeRelativeDirPath = StrUtil.isBlank(relativeDirPath) ? "." : relativeDirPath.trim();
        try {
            Path path = resolveProjectPath(appId, safeRelativeDirPath);
            if (!Files.isDirectory(path)) {
                return "错误：目录不存在或不是目录 - " + safeRelativeDirPath;
            }
            List<DirectoryEntry> entries = collectEntries(path);
            StringBuilder structure = new StringBuilder();
            structure.append("项目目录结构:\n");
            entries.stream()
                    .sorted(Comparator.comparing(entry -> entry.relativePath().toString()))
                    .limit(MAX_ENTRIES)
                    .forEach(entry -> {
                        int depth = Math.max(0, entry.relativePath().getNameCount() - 1);
                        structure.append("  ".repeat(depth))
                                .append(entry.relativePath().getFileName());
                        if (entry.directory()) {
                            structure.append('/');
                        }
                        structure.append('\n');
                    });
            if (entries.size() > MAX_ENTRIES) {
                structure.append("... 已省略 ")
                        .append(entries.size() - MAX_ENTRIES)
                        .append(" 个条目\n");
            }
            return structure.toString();

        } catch (Exception e) {
            String errorMessage = "读取目录结构失败: " + safeRelativeDirPath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    private List<DirectoryEntry> collectEntries(Path root) throws IOException {
        List<DirectoryEntry> entries = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(root) && shouldIgnore(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (!dir.equals(root)) {
                    entries.add(new DirectoryEntry(root.relativize(dir), true));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!shouldIgnore(file.getFileName().toString())) {
                    entries.add(new DirectoryEntry(root.relativize(file), false));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return entries;
    }

    /**
     * 判断是否应该忽略该文件或目录
     */
    private boolean shouldIgnore(String fileName) {
        // 检查是否在忽略名称列表中
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }

        // 检查文件扩展名
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

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
        String relativeDirPath = arguments.getStr("relativeDirPath");
        if (StrUtil.isEmpty(relativeDirPath)) {
            relativeDirPath = "根目录";
        }
        return String.format("[工具调用] %s %s", getDisplayName(), relativeDirPath);
    }
}
