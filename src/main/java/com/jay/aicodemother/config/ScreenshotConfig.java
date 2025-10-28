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
     * 每天凌晨两天进行清理过期的临时截图文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanUpTemScreenshots() {
        // 定时清理临时图片
        log.info("开始清理临时图片...");
        String rootPath = System.getProperty("user.dir") + "/tmp/screenshots";
        FileUtil.clean(rootPath);
        log.info("临时图片清理完成...");
    }

}