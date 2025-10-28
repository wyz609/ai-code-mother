package com.jay.aicodemother.manager;

import com.jay.aicodemother.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.exception.CosClientException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * COS对象存储管理器
 *
 * @author yupi
 */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        try {
            // 验证参数
            if (key == null || key.isEmpty()) {
                log.error("COS上传失败：key不能为空");
                return null;
            }
            
            if (file == null || !file.exists()) {
                log.error("COS上传失败：文件不存在，key: {}", key);
                return null;
            }
            
            // 确保key以/开头，但不以/结尾
            if (!key.startsWith("/")) {
                key = "/" + key;
            }
            
            log.info("准备上传文件到COS: bucket={}, key={}, file={}", 
                    cosClientConfig.getBucket(), key, file.getAbsolutePath());
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
            PutObjectResult result = cosClient.putObject(putObjectRequest);
            log.info("文件上传COS成功: bucket={}, key={}", cosClientConfig.getBucket(), key);
            return result;
        } catch (CosClientException e) {
            log.error("COS上传失败：bucket={}, key={}, error={}", 
                    cosClientConfig.getBucket(), key, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("COS上传发生未知错误：bucket={}, key={}, error={}", 
                    cosClientConfig.getBucket(), key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 上传文件到 COS 并返回访问 URL
     *
     * @param key  COS对象键（完整路径）
     * @param file 要上传的文件
     * @return 文件的访问URL，失败返回null
     */
    public String uploadFile(String key, File file) {
        // 上传文件
        PutObjectResult result = putObject(key, file);
        if (result != null) {
            // 构建访问URL，确保host末尾没有/，key开头有/
            String host = cosClientConfig.getHost();
            if (host.endsWith("/")) {
                host = host.substring(0, host.length() - 1);
            }
            
            if (!key.startsWith("/")) {
                key = "/" + key;
            }
            
            String url = String.format("%s%s", host, key);
            log.info("文件上传COS成功: {} -> {}", file.getName(), url);
            return url;
        } else {
            log.error("文件上传COS失败，返回结果为空，key: {}, file: {}", key, file.getAbsolutePath());
            return null;
        }
    }
}