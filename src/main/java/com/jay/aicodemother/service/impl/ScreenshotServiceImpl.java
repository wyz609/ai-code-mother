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

    // 对象存储管理器：用于上传截图
    private final  CosManager cosManager;

    /**
     * 生成网页截图并上传到对象存储，返回可访问的图片 URL
     */
    @Override
    public String generateAndUploadScreenshot(String webUrl){
        // 参数校验：截图地址不能为空
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR,"截图的地址不能为空");
        // 记录开始截图日志
        log.info("开始生成网页截图, URL: {}", webUrl);
        // 本地截图：用浏览器打开页面并保存截图到本地临时文件
        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        // 截图失败则抛操作异常
        ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath), ErrorCode.OPERATION_ERROR,"生成网页截图失败");
        // 上传图片到 COS
        try{
            // 调用私有方法上传本地截图，返回 COS URL
            String cosUrl = uploadScreenshotToCos(localScreenshotPath);
            // 上传失败则抛异常
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.OPERATION_ERROR,"上传图片到 COS 失败");
            // 记录上传成功日志
            log.info("上传图片到 COS 成功，COS URL: {}", cosUrl);
            // 返回可访问的图片 URL
            return cosUrl;
        }finally {
            // 清除本地文件（无论成功失败都清理临时截图）
            cleanUpLocalFile(localScreenshotPath);
        }

    }

    /**
     * 清除本地文件
     * @param localScreenshotPath 待清除文件的路径
     */
    private void cleanUpLocalFile(String localScreenshotPath) {
        // 路径为空则无需清理
        if (StrUtil.isBlank(localScreenshotPath)) {
            return;
        }
        
        // 创建文件对象
        File file = new File(localScreenshotPath);
        // 文件存在才删除
        if (file.exists()){
            FileUtil.del(file);
            // 记录清理日志
            log.info("清理本地文件成功: {}", localScreenshotPath);
        }
    }

    /**
     * 上传图片到 对象存储
     * @param localScreenshotPath 本地截图路劲
     * @return 对象存储访问 URL， 失败则返回 null
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        // 本地路径为空则上传失败
        if(StrUtil.isBlank(localScreenshotPath)){
            log.error("上传截图到COS失败：本地截图路径为空");
            return null;
        }
        // 创建文件对象
        File screenshotFile = new File(localScreenshotPath);
        // 文件不存在则上传失败
        if(!screenshotFile.exists()){
            log.error("截图文件不存在: {}",localScreenshotPath );
            return null;
        }
        // 生成 COS 对象键：随机 8 位文件名 + 压缩后缀
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compress.jpg";
        // 生成按日期分层的对象键
        String cosKey = generateScreenshotKey(fileName);
        // 记录上传日志
        log.info("准备上传截图到COS，key: {}, file: {}", cosKey, localScreenshotPath);
        // 调用对象存储管理器上传文件，返回访问 URL
        return cosManager.uploadFile(cosKey, screenshotFile);
    }

    /**
     * 生成截图的 COS 对象键 格式为 /screenshots/年/月/日/文件名
     * @param fileName 文件名
     * @return
     */
    private String generateScreenshotKey(String fileName) {
        // 取当前日期，格式化为 年/月/日 目录层级
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // 确保路径以/开头，不以/结尾
        String key = String.format("screenshots/%s/%s", datePath, fileName);
        // 记录生成的键（调试级别）
        log.debug("生成COS对象键: {}", key);
        return key;
    }

}