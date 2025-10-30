package com.jay.aicodemother.ai.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Class name: ToolManage
 * Package: com.jay.aicodemother.ai.tools
 * Description:
 *
 * @Create: 2025/10/30 9:00
 * @Author: jay
 * @Version: 1.0
 */
@Slf4j
@Component
public class ToolManage {

    // 定义根据工具名称映射到工具的 Map
    private final Map<String, BaseTool> toolMap = new HashMap<>();

    // 自动注入所有工具类
    @Resource
    private BaseTool[] tools;

    // 初始化工具映射
    @PostConstruct
    public void init() {
        for(BaseTool tool : tools){
            toolMap.put(tool.getToolName(), tool);
            log.info("已加载工具：{} -> {}", tool.getToolName(), tool.getDisplayName());
        }
        log.info("工具管理器加载完成,已加载 {} 个工具", toolMap.size());
    }

    /**
     * 根据工具名称获取工具
     * @param toolName
     * @return
     */
    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    /**
     * 获取所有工具
     * @return
     */
    public BaseTool[] getTools() {
        return tools;
    }

}