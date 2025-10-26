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
 * 流式输出响应基类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamMessage {
    private String type;
}