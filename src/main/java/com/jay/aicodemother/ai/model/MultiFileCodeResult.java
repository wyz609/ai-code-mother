package com.jay.aicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 多文件代码生成结果
 *
 * <p>AI 模型结构化输出结果：包含 HTML/CSS/JS 三个文件代码与生成说明。</p>
 */
@Description("生成多个代码文件的结果")
@Data
public class MultiFileCodeResult {

    // HTML 代码（index.html）
    @Description("HTML代码")
    private String htmlCode;

    // CSS 代码（style.css）
    @Description("CSS代码")
    private String cssCode;

    // JS 代码（script.js）
    @Description("JS代码")
    private String jsCode;

    // 代码生成说明/描述
    @Description("生成代码的描述")
    private String description;
}

