package com.jay.aicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 对话历史消息类型枚举
 *
 * @author <a href="https://github.com/wyz609">程序员阿阳</a>
 */
@Getter
public enum ChatHistoryMessageTypeEnum {

    // 用户消息
    USER("用户消息", "user"),
    // AI 消息
    AI("AI消息", "ai"),
    // 错误消息
    ERROR("错误消息", "error");

    // 消息类型中文说明
    private final String text;
    // 消息类型值（存入数据库）
    private final String value;

    ChatHistoryMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static ChatHistoryMessageTypeEnum getEnumByValue(String value) {
        // 空值返回 null
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        // 遍历匹配 value
        for (ChatHistoryMessageTypeEnum anEnum : ChatHistoryMessageTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        // 未匹配返回 null
        return null;
    }
}
