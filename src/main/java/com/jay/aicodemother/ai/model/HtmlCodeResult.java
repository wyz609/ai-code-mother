package com.jay.aicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;
/**
 * 单文件（HTML）生成结果
 *
 * <p>AI 模型结构化输出结果：包含完整 HTML 代码与生成说明。</p>
 */
@Description("生成 HTML 代码文件的结果")
@Data
public class HtmlCodeResult {

    // 完整 HTML 代码内容
    @Description("HTML代码")
    private String htmlCode;

    // 代码生成说明/描述
    @Description("生成代码的描述")
    private String description;
}

