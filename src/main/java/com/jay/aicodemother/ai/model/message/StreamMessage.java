/**
 * Class name: StreamMessage
 * Package: com.jay.aicodemother.ai.model.message
 * Description:
 *
 * @Create: 2025/10/25 21:02
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.ai.model.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式输出响应消息基类
 *
 * <p>所有 SSE 消息的父类，通过 type 字段标识消息类型
 * （ai_response 响应 / tool_request 工具请求 / tool_executed 工具执行结果）。</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamMessage {
    // 消息类型（对应 StreamMessageTypeEnum 的 value）
    private String type;
}