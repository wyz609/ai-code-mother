package com.jay.aicodemother.ai.model.message;

import lombok.Getter;

/**
 * 流式消息类型枚举（SSE 推送给前端的不同事件类型）
 */
@Getter
public enum StreamMessageTypeEnum {

    // AI 文本响应
    AI_RESPONSE("ai_response", "AI响应"),
    // 工具调用请求
    TOOL_REQUEST("tool_request", "工具请求"),
    // 工具执行完成结果
    TOOL_EXECUTED("tool_executed", "工具执行结果");

    // 消息类型值（序列化到 JSON）
    private final String value;
    // 消息类型中文说明
    private final String text;

    StreamMessageTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获取枚举
     */
    public static StreamMessageTypeEnum getEnumByValue(String value) {
        // 遍历匹配 value
        for (StreamMessageTypeEnum typeEnum : values()) {
            if (typeEnum.getValue().equals(value)) {
                return typeEnum;
            }
        }
        // 未匹配返回 null
        return null;
    }
}
