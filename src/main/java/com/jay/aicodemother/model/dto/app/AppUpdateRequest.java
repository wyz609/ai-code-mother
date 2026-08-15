package com.jay.aicodemother.model.dto.app;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用更新请求（用户版）
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * 应用ID
     */
    @NotNull(message = "应用ID不能为空")
    private Long id;

    /**
     * 应用名称
     * 限制：长度 1-100 字符
     */
    @Size(min = 1, max = 100, message = "应用名称长度必须在 1-100 字符之间")
    private String appName;

    private static final long serialVersionUID = 1L;
}
