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
 * Class name: CosClientConfig
 * Package: com.jay.aicodemother.config
 * Description:
 *
 * @Create: 2025/10/27 15:42
 * @Author: jay
 * @Version: 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {

    /*
    域名
     */
    private String host;

    /*
    密钥
     */
    private String secretId;

    /*
    密钥 （千万不能泄露该密钥信息）
     */
    private String secretKey;

    /*
    存储桶名称
     */
    private String bucket;

    /*
    存储桶区域
     */
    private String region;

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