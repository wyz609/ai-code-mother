package com.jay.aicodemother.constant;

import cn.hutool.core.util.StrUtil;

/**
 * Class name: AppConstant
 * Package: com.jay.aicodemother.constant
 * Description:
 *
 * @Create: 2025/10/25 15:54
 * @Author: jay
 * @Version: 1.0
 */
public interface AppConstant {

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + System.getProperty("file.separator") + "tmp" + System.getProperty("file.separator") + "code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + System.getProperty("file.separator") + "tmp" + System.getProperty("file.separator") + "code_deploy";

    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";

}