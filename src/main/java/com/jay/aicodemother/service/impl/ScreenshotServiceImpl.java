package com.jay.aicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.exception.ThrowUtils;
import com.jay.aicodemother.manager.CosManager;
import com.jay.aicodemother.service.ScreenshotService;
import com.jay.aicodemother.utils.WebScreenshotUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Class name: ScreenshotServiceImpl
 * Package: com.jay.aicodemother.service.impl
 * Description:
 *
 * @Create: 2025/10/27 15:58
 * @Author: jay
 * @Version: 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScreenshotServiceImpl implements ScreenshotService {

    private final  CosManager cosManager;

    @Override
    public String generateAndUploadScreenshot(String webUrl){
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR,"截图的地址不能为空");
        log.info("开始生成网页截图, URL: {}", webUrl);
        // 本地截图
        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath), ErrorCode.OPERATION_ERROR,"生成网页截图失败");
        // 上传图片到 COS
        try{
            String cosUrl = uploadScreenshotToCos(localScreenshotPath);
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.OPERATION_ERROR,"上传图片到 COS 失败");
            log.info("上传图片到 COS 成功，COS URL: {}", cosUrl);
            return cosUrl;
        }finally {
            // 清除本地文件
            cleanUpLocalFile(localScreenshotPath);
        }

    }

    /**
     * 清除本地文件
     * @param localScreenshotPath 待清除文件的路径
     */
    private void cleanUpLocalFile(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return;
        }
        
        File file = new File(localScreenshotPath);
        if (file.exists()){
            FileUtil.del(file);
            log.info("清理本地文件成功: {}", localScreenshotPath);
        }
    }

    /**
     * 上传图片到 对象存储
     * @param localScreenshotPath 本地截图路劲
     * @return 对象存储访问 URL， 失败则返回 null
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        if(StrUtil.isBlank(localScreenshotPath)){
            log.error("上传截图到COS失败：本地截图路径为空");
            return null;
        }
        File screenshotFile = new File(localScreenshotPath);
        if(!screenshotFile.exists()){
            log.error("截图文件不存在: {}",localScreenshotPath );
            return null;
        }
        // 生成 COS 对象键
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compress.jpg";
        String cosKey = generateScreenshotKey(fileName);
        log.info("准备上传截图到COS，key: {}, file: {}", cosKey, localScreenshotPath);
        return cosManager.uploadFile(cosKey, screenshotFile);
    }

    /**
     * 生成截图的 COS 对象键 格式为 /screenshots/年/月/日/文件名
     * @param fileName 文件名
     * @return
     */
    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // 确保路径以/开头，不以/结尾
        String key = String.format("screenshots/%s/%s", datePath, fileName);
        log.debug("生成COS对象键: {}", key);
        return key;
    }

}