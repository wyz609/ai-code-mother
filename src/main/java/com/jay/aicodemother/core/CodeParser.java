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
        // 创建结果对象
        HtmlCodeResult result = new HtmlCodeResult();

        // 匹配HTML代码块（## index.html 后跟 ```html
        Pattern htmlPattern = Pattern.compile("## index\\.html\\s*```html([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher htmlMatcher = htmlPattern.matcher(codeContent);

        // 优先匹配带标题的 Markdown 代码块格式
        if (htmlMatcher.find()) {
            // 取代码块内容并去除首尾空白
            result.setHtmlCode(htmlMatcher.group(1).trim());
        } else {
            // 回退到原始HTML正则匹配（无标题时直接匹配整个 HTML 文档）
            Pattern fallbackPattern = Pattern.compile("<!DOCTYPE html>[\\s\\S]*?</html>", Pattern.CASE_INSENSITIVE);
            Matcher fallbackMatcher = fallbackPattern.matcher(codeContent);
            // 命中则提取完整 HTML 文档
            if (fallbackMatcher.find()) {
                result.setHtmlCode(fallbackMatcher.group().trim());
            }
        }

        // 提取描述信息（代码块前的说明文字）
        String description = extractDescription(codeContent, result.getHtmlCode());
        result.setDescription(description);

        // 返回解析结果
        return result;
    }

    /**
     * 解析多文件代码，包含HTML、CSS、JS
     * @param codeContent 原始内容
     * @return MultiFileCodeResult 结果
     */
    public static MultiFileCodeResult parseMultiFileCode(String codeContent) {
        // 创建结果对象
        MultiFileCodeResult result = new MultiFileCodeResult();

        // 提取HTML代码：匹配 ## index.html + ```html 代码块
        Pattern htmlPattern = Pattern.compile("## index\\.html\\s*```html([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher htmlMatcher = htmlPattern.matcher(codeContent);
        // 命中则提取
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

        // 返回解析结果
        return result;
    }

    /**
     * 提取CSS代码（## style.css 后跟 ```css ... ```）
     * @param content 原始内容
     * @return CSS代码
     */
    private static String extractCssCode(String content) {
        // 匹配 style.css 代码块
        Pattern cssPattern = Pattern.compile("## style\\.css\\s*```css([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher cssMatcher = cssPattern.matcher(content);
        // 命中则返回 CSS 内容
        if (cssMatcher.find()) {
            return cssMatcher.group(1).trim();
        }
        // 未命中返回 null
        return null;
    }

    /**
     * 提取JS代码（## script.js 后跟 ```javascript ... ```）
     * @param content 原始内容
     * @return JS代码
     */
    private static String extractJsCode(String content) {
        // 匹配 script.js 代码块
        Pattern jsPattern = Pattern.compile("## script\\.js\\s*```javascript([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher jsMatcher = jsPattern.matcher(content);
        // 命中则返回 JS 内容
        if (jsMatcher.find()) {
            return jsMatcher.group(1).trim();
        }
        // 未命中返回 null
        return null;
    }

    /**
     * 提取描述信息（从开头到第一个代码块前，包括标题和附加说明）
     * @param content 原始内容
     * @param codeContent 代码内容（用于定位分界）
     * @return 描述信息
     */
    private static String extractDescription(String content, String codeContent) {
        // 无代码内容时整个文本视为描述
        if (codeContent == null) {
            return content.trim();
        }

        // 查找第一个代码块标题的位置（## index.html）
        int codeIndex = content.toLowerCase().indexOf("## index.html");
        // 找到标题则截取标题前的文本作为描述
        if (codeIndex > 0) {
            String description = content.substring(0, codeIndex).trim();
            // 清理多余标记（如 # 生成的网站代码）
            description = description.replaceAll("# 生成的网站代码", "").trim();
            // 追加附加说明部分（如果存在，从最后一个代码块后提取）
            int lastCodeEnd = content.lastIndexOf("```");
            // 最后一个代码块后还有内容，且以"### 附加说明"开头时追加
            if (lastCodeEnd > 0 && lastCodeEnd < content.length() - 1) {
                String additional = content.substring(lastCodeEnd + 3).trim();
                if (additional.startsWith("### 附加说明")) {
                    description += "\n" + additional;
                }
            }
            // 描述为空时返回"无描述"
            return description.isEmpty() ? "无描述" : description;
        }

        // 回退到原始逻辑（如果无标题）：截取 <!DOCTYPE html> 之前的内容
        int htmlIndex = content.indexOf("<!DOCTYPE html>");
        if (htmlIndex > 0) {
            // 去除"html 格式"等干扰文本
            return content.substring(0, htmlIndex).trim().replaceAll("html\\s+格式", "").trim();
        }

        // 兜底返回无描述
        return "无描述";
    }
}