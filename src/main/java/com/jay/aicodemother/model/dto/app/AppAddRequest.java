package com.jay.aicodemother.model.dto.app;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 */
@Data
public class AppAddRequest implements Serializable {

//    /**
//     * 应用名称
//     */
//    private String appName;

    /**
     * 应用初始化的 prompt
     * 限制：不能为空，长度 1-2000 字符
     */
    @NotBlank(message = "初始化提示词不能为空")
    @Size(min = 1, max = 2000, message = "初始化提示词长度必须在 1-2000 字符之间")
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     * 可选值：html, multi_file, vue_project
     */
    @Size(max = 20, message = "代码生成类型长度不能超过20字符")
    private String codeGenType;

    private static final long serialVersionUID = 1L;
}
