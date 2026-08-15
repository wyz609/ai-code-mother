/**
 * Class name: AppDeployRequest
 * Package: com.jay.aicodemother.model.dto.app
 * Description: 应用部署请求
 *
 * @Create: 2025/9/24 19:36
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.model.dto.app;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppDeployRequest implements Serializable {

    /**
     * 应用ID
     */
    @NotNull(message = "应用ID不能为空")
    @Positive(message = "应用ID必须为正数")
    private Long appId;

    private static final long serialVersionUID = 1L;
}