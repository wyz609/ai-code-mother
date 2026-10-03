package com.jay.aicodemother.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 项目源文件 VO（供前端"查看代码"面板使用）。
 */
@Data
@AllArgsConstructor
public class ProjectFileVO implements Serializable {

    // 文件相对路径（如 src/App.vue）
    private String path;

    // 文件语言类型（用于代码高亮，如 vue/javascript/css）
    private String language;

    // 文件完整内容
    private String content;

    private static final long serialVersionUID = 1L;
}
