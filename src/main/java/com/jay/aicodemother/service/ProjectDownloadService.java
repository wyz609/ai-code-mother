package com.jay.aicodemother.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Class name: ProjectDownloadService
 * Package: com.jay.aicodemother.service
 * Description:
 *
 * @Create: 2025/10/27 19:07
 * @Author: jay
 * @Version: 1.0
 */
public interface ProjectDownloadService {
    // 下载压缩项目
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}