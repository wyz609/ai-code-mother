package com.jay.aicodemother.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 对象存储客户端配置类。
 *
 * <p>从 application.yml 的 cos.client 前缀读取配置，并创建 COSClient Bean，
 * 用于上传截图等静态资源。</p>
 */
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {

    /*
    域名（访问 URL 前缀）
     */
    private String host;

    /*
    访问密钥 ID
     */
    private String secretId;

    /*
    访问密钥 Key（千万不能泄露该密钥信息）
     */
    private String secretKey;

    /*
    存储桶名称
     */
    private String bucket;

    /*
    存储桶区域（如 ap-guangzhou）
     */
    private String region;

    /**
     * 创建 COS 客户端 Bean
     */
    @Bean
    public COSClient cosClient(){
        // 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 设置 bucket 区域，COS地域
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 生成 cos 客户端
        return new COSClient(cred, clientConfig);
    }
}