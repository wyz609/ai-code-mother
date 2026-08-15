package com.jay.aicodemother.ai.tools;

import com.jay.aicodemother.constant.AppConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDirReadToolTest {

    private static final long APP_ID = 991001L;
    private final Path projectRoot = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + APP_ID);

    @AfterEach
    void cleanUp() throws IOException {
        if (!Files.exists(projectRoot)) {
            return;
        }
        try (var paths = Files.walk(projectRoot)) {
            paths.sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> path.toFile().delete());
        }
    }

    @Test
    void blankPathReadsProjectRootWithoutTraversingDependencies() throws IOException {
        Files.createDirectories(projectRoot.resolve("src"));
        Files.writeString(projectRoot.resolve("src/App.vue"), "<template />");
        Files.createDirectories(projectRoot.resolve("node_modules/pkg"));
        Files.writeString(projectRoot.resolve("node_modules/pkg/index.js"), "ignored");

        String result = new FileDirReadTool().readDir("  ", APP_ID);

        assertTrue(result.contains("src/"));
        assertTrue(result.contains("App.vue"));
        assertFalse(result.contains("文件路径不能为空"));
        assertFalse(result.contains("node_modules"));
        assertFalse(result.contains("index.js"));
    }
}
