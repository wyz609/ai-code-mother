package com.jay.aicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户角色枚举。
 */
@Getter
public enum UserRoleEnum {

    // 普通用户
    USER("用户", "user"),
    // 管理员
    ADMIN("管理员", "admin");

    // 角色中文名称
    private final String text;

    // 角色值（存入数据库/用于权限判断）
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        // 空值返回 null
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        // 遍历所有枚举匹配 value
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        // 未匹配返回 null
        return null;
    }
}
