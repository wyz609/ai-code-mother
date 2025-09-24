/**
 * Class name: AppDeployRequest
 * Package: com.jay.aicodemother.model.dto.app
 * Description:
 *
 * @Create: 2025/9/24 19:36
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;
@Data
public class AppDeployRequest implements Serializable {
    /**
     * 应用ID
     */
    private Long appId;

    private static final long serialVersionUID = 1L;

}