package com.jay.aicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileToolPresentationTest {

    @Test
    void writeFileResultDoesNotIncludeFileContent() {
        String source = "<template><main>large source payload</main></template>";

        String result = new FileWriteTool().generateToolExecutedResult(new JSONObject()
                .set("relativeFilePath", "src/App.vue")
                .set("content", source));

        assertTrue(result.contains("写入文件 src/App.vue"));
        assertTrue(result.contains(source.length() + " 个字符"));
        assertFalse(result.contains(source));
    }

    @Test
    void modifyFileResultDoesNotIncludeBeforeOrAfterContent() {
        String oldContent = "const theme = 'light'";
        String newContent = "const theme = 'dark'";

        String result = new FileModifyTool().generateToolExecutedResult(new JSONObject()
                .set("relativeFilePath", "src/theme.js")
                .set("oldContent", oldContent)
                .set("newContent", newContent));

        assertTrue(result.contains("修改文件 src/theme.js"));
        assertTrue(result.contains(oldContent.length() + " -> " + newContent.length()));
        assertFalse(result.contains(oldContent));
        assertFalse(result.contains(newContent));
    }
}
