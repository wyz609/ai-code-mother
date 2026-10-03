package com.jay.aicodemother.config;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Class name: ScreenshotConfig
 * Package: com.jay.aicodemother.config
 * Description:
 *
 * @Create: 2025/10/27 16:40
 * @Author: jay
 * @Version: 1.0
 */
@Configuration
@EnableScheduling
@Slf4j
public class ScreenshotConfig {

    /**
     * 每天凌晨两点进行清理过期的临时截图文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanUpTemScreenshots() {
        // 定时清理临时图片：记录开始日志
        log.info("开始清理临时图片...");
        // 临时截图存放目录
        String rootPath = System.getProperty("user.dir") + "/tmp/screenshots";
        // 清空整个临时目录
        FileUtil.clean(rootPath);
        // 记录清理完成日志
        log.info("临时图片清理完成...");
    }

}