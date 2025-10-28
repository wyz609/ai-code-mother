package com.jay.aicodemother.ai;

import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

/**
 * Class name: AiCodeGenTypeRoutingService
 * Package: com.jay.aicodemother.ai
 * Description:
 *
 * @Create: 2025/10/27 20:34
 * @Author: jay
 * @Version: 1.0
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 根据用户需求智能选择代码生成类型
     * @param userPrompt 用户输入的需求描述
     * @return 推荐的代码生成类型
     */
    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(String userPrompt);

}