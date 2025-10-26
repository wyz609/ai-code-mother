/**
 * Class name: HtmlCodeParser
 * Package: com.jay.aicodemother.parser
 * Description:
 *
 * @Create: 2025/9/23 17:47
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.parser;

import com.jay.aicodemother.ai.model.HtmlCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 单文件 HTML 解析器
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {


    /**
     * 解析HTML代码（单文件模式）
     * @param codeContent 原始内容
     * @return HtmlCodeResult 结果
     */
    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();

        // 如果输入为空，直接返回空结果
        if (codeContent == null || codeContent.trim().isEmpty()) {
            result.setHtmlCode("");
            result.setDescription("未生成任何代码内容");
            return result;
        }

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