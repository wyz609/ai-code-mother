/**
 * Class name: MultiFileCodeParser
 * Package: com.jay.aicodemother.parser
 * Description:
 *
 * @Create: 2025/9/23 17:51
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.parser;


import com.jay.aicodemother.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {
    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();

        // 提取HTML代码
        Pattern htmlPattern = Pattern.compile("## index\\.html\\s*```html([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher htmlMatcher = htmlPattern.matcher(codeContent);
        if (htmlMatcher.find()) {
            result.setHtmlCode(htmlMatcher.group(1).trim());
        }

        // 提取CSS代码
        String cssCode = extractCssCode(codeContent);
        result.setCssCode(cssCode);

        // 提取JS代码
        String jsCode = extractJsCode(codeContent);
        result.setJsCode(jsCode);

        // 提取描述信息
        String description = extractDescription(codeContent, result.getHtmlCode());
        result.setDescription(description);

        return result;
    }

    /**
     * 提取CSS代码（## style.css 后跟 ```css ... ```）
     * @param content 原始内容
     * @return CSS代码
     */
    private static String extractCssCode(String content) {
        Pattern cssPattern = Pattern.compile("## style\\.css\\s*```css([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher cssMatcher = cssPattern.matcher(content);
        if (cssMatcher.find()) {
            return cssMatcher.group(1).trim();
        }
        return null;
    }

    /**
     * 提取JS代码（## script.js 后跟 ```javascript ... ```）
     * @param content 原始内容
     * @return JS代码
     */
    private static String extractJsCode(String content) {
        Pattern jsPattern = Pattern.compile("## script\\.js\\s*```javascript([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher jsMatcher = jsPattern.matcher(content);
        if (jsMatcher.find()) {
            return jsMatcher.group(1).trim();
        }
        return null;
    }

    /**
     * 提取描述信息（从开头到第一个代码块前，包括标题和附加说明）
     * @param content 原始内容
     * @param codeContent 代码内容（用于定位分界）
     * @return 描述信息
     */
    private static String extractDescription(String content, String codeContent) {
        if (codeContent == null) {
            return content.trim();
        }

        // 查找第一个代码块标题的位置（## index.html）
        int codeIndex = content.toLowerCase().indexOf("## index.html");
        if (codeIndex > 0) {
            String description = content.substring(0, codeIndex).trim();
            // 清理多余标记（如 # 生成的网站代码）
            description = description.replaceAll("# 生成的网站代码", "").trim();
            // 追加附加说明部分（如果存在，从最后一个代码块后提取）
            int lastCodeEnd = content.lastIndexOf("```");
            if (lastCodeEnd > 0 && lastCodeEnd < content.length() - 1) {
                String additional = content.substring(lastCodeEnd + 3).trim();
                if (additional.startsWith("### 附加说明")) {
                    description += "\n" + additional;
                }
            }
            return description.isEmpty() ? "无描述" : description;
        }

        // 回退到原始逻辑（如果无标题）
        int htmlIndex = content.indexOf("<!DOCTYPE html>");
        if (htmlIndex > 0) {
            return content.substring(0, htmlIndex).trim().replaceAll("html\\s+格式", "").trim();
        }

        return "无描述";
    }
}