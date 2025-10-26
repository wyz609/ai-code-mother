/**
 * Class name: AIResponseMessage
 * Package: com.jay.aicodemother.ai.model.message
 * Description: AI 响应信息
 *
 * @Create: 2025/10/25 21:04
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.ai.model.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AIResponseMessage extends StreamMessage{

    private String data;

    public AIResponseMessage(String data) {
        super(StreamMessageTypeEnum.AI_RESPONSE.getValue());
        this.data = data;
    }
}