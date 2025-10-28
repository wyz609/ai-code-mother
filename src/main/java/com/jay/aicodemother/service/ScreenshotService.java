package com.jay.aicodemother.service;

import com.mybatisflex.core.service.IService;

/**
 * Class name: ScreenshotService
 * Package: com.jay.aicodemother.service
 * Description:
 *
 * @Create: 2025/10/27 15:57
 * @Author: jay
 * @Version: 1.0
 */
public interface ScreenshotService{
    // 进行截图并上传截图文件到对象存储
    String generateAndUploadScreenshot(String webUrl);
}