package com.jay.aicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 代码生成类型枚举
 */
@Getter
public enum CodeGenTypeEnum {

    // 原生 HTML 模式：单页面
    HTML("原生 HTML 模式", "html"),
    // 原生多文件模式：HTML + CSS + JS 多文件静态站
    MULTI_FILE("原生多文件模式", "multi_file"),
    // Vue 工程模式：完整 Vue 工程（可打包部署）
    VUE_PROJECT("Vue 工程模式", "vue_project");

    // 枚举中文说明
    private final String text;
    // 枚举值（存入数据库/用于路由分发）
    private final String value;

    CodeGenTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static CodeGenTypeEnum getEnumByValue(String value) {
        // 空值返回 null
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        // 遍历匹配 value
        for (CodeGenTypeEnum anEnum : CodeGenTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        // 未匹配返回 null
        return null;
    }
}
