package com.jay.aicodemother.core;

import com.jay.aicodemother.ai.model.HtmlCodeResult;
import com.jay.aicodemother.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码解析器，用于解析大模型响应文本文件中的HTML、CSS、JS代码。
 * 支持两种模式：纯HTML（单文件）和多文件（HTML + CSS + JS）。
 * 优化为解析Markdown格式的输出，兼容## 文件名 + ```language 结构。
 */
public class CodeParser {

    /**
     * 解析HTML代码（单文件模式）
     * @param codeContent 原始内容
     * @return HtmlCodeResult 结果
     */
    public static HtmlCodeResult parseHtmlCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();

        // 匹配HTML代码块（## index.html 后跟 ```html
        Pattern htmlPattern = Pattern.compile("## index\\.html\\s*```html([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher htmlMatcher = htmlPattern.matcher(codeContent);

        if (htmlMatcher.find()) {
            result.setHtmlCode(htmlMatcher.group(1).trim());
        } else {
            // 回退到原始HTML正则匹配
            Pattern fallbackPattern = Pattern.compile("<!DOCTYPE html>[\\s\\S]*?</html>", Pattern.CASE_INSENSITIVE);
            Matcher fallbackMatcher = fallbackPattern.matcher(codeContent);
            if (fallbackMatcher.find()) {
                result.setHtmlCode(fallbackMatcher.group().trim());
            }
        }

        // 提取描述信息
        String description = extractDescription(codeContent, result.getHtmlCode());
        result.setDescription(description);

        return result;
    }

    /**
     * 解析多文件代码，包含HTML、CSS、JS
     * @param codeContent 原始内容
     * @return MultiFileCodeResult 结果
     */
    public static MultiFileCodeResult parseMultiFileCode(String codeContent) {
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