package com.jay.aicodemother.ai.model.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Class name: ToolRequestMessage
 * Package: com.jay.aicodemother.ai.model.message
 * Description: 工具调用消息
 *
 * @Create: 2025/10/25 21:08
 * @Author: jay
 * @Version: 1.0
 */

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolRequestMessage extends StreamMessage{

    private String id;

    private String name;

    private String arguments;

    public ToolRequestMessage(ToolExecutionRequest toolExecutionRequest) {
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        this.id = toolExecutionRequest.id();
        this.name = toolExecutionRequest.name();
        this.arguments = toolExecutionRequest.arguments();
    }

}