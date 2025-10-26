package com.jay.aicodemother.ai.model.message;

import dev.langchain4j.service.tool.ToolExecution;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Class name: ToolExecutedMessage
 * Package: com.jay.aicodemother.ai.model.message
 * Description:
 *
 * @Create: 2025/10/25 21:12
 * @Author: jay
 * @Version: 1.0
 */

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolExecutedMessage extends StreamMessage{

    private String id;

    private String name;;

    private String arguments;

    private String result;

    public ToolExecutedMessage(ToolExecution toolExecution){
        super(StreamMessageTypeEnum.TOOL_EXECUTED.getValue());
        this.id = toolExecution.request().id();
        this.name = toolExecution.request().name();
        this.arguments = toolExecution.request().arguments();
        this.result = toolExecution.result();
    }
}