package com.jay.aicodemother.core;

import com.jay.aicodemother.ai.model.HtmlCodeResult;
import com.jay.aicodemother.ai.model.MultiFileCodeResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Slf4j
class CodeParserTest {

    @Test
    void parseHtmlCode() {
        String codeContent = """
            随便写一段描述：
            html 格式
            <!DOCTYPE html>
            <html>
            <head>
                <title>测试页面</title>
            </head>
            <body>
                <h1>Hello World!</h1>
            </body>
            </html>
    
            随便写一段描述
            """;
        HtmlCodeResult result = CodeParser.parseHtmlCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
    }

    @Test
    void parseMultiFileCode() {
        String codeContent = """
                这个打卡网页包含一个简单的打卡功能，用户点击按钮即可记录打卡时间，并显示历史打卡记录。

                ## index.html
                ```html
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>每日打卡</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <div class="container">
                        <h1>每日打卡</h1>
                        <button id="checkInBtn">点击打卡</button>
                        <div id="history" class="history"></div>
                    </div>
                    <script src="script.js"></script>
                </body>
                </html>
                ```

                ## style.css
                ```css
                body {
                    font-family: Arial, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background-color: #f5f5f5;
                }

                .container {
                    max-width: 400px;
                    margin: 0 auto;
                    background: white;
                    padding: 20px;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }

                h1 {
                    text-align: center;
                    color: #333;
                }

                #checkInBtn {
                    display: block;
                    width: 100%;
                    padding: 10px;
                    background: #4CAF50;
                    color: white;
                    border: none;
                    border-radius: 4px;
                    font-size: 16px;
                    cursor: pointer;
                    margin-bottom: 20px;
                }

                #checkInBtn:hover {
                    background: #45a049;
                }

                .history {
                    max-height: 200px;
                    overflow-y: auto;
                }

                .history-item {
                    padding: 8px;
                    border-bottom: 1px solid #eee;
                }
                ```

                ## script.js
                ```javascript
                // 初始化历史记录
                let history = JSON.parse(localStorage.getItem('checkInHistory')) || [];

                // 更新历史记录显示
                function updateHistory() {
                    const historyEl = document.getElementById('history');
                    historyEl.innerHTML = history.map(item =>\s
                        `<div class="history-item">${item}</div>`
                    ).join('');
                }

                // 打卡功能
                document.getElementById('checkInBtn').addEventListener('click', () => {
                    const now = new Date().toLocaleString();
                    history.unshift(now);
                    localStorage.setItem('checkInHistory', JSON.stringify(history));
                    updateHistory();
                });

                // 页面加载时显示历史记录
                updateHistory();
                ```
                """;
        MultiFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
        log.info("生成的HTML代码, {}", result.getHtmlCode());
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
        assertNotNull(result.getCssCode());
        assertNotNull(result.getJsCode());

        log.info("<================>");
        log.info("生成的CSS代码, {}", result.getCssCode());
        log.info("生成的JS代码, {}", result.getJsCode());
    }
}