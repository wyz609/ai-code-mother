/**
 * Class name: ProxyConfig
 * Package: com.jay.aicodemother.config
 * Description: HTTP 代理配置，用于访问外部 API
 *
 * @Create: 2026/7/22
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "proxy")
@Data
public class ProxyConfig {

    /**
     * 是否启用代理
     */
    private boolean enabled = false;

    /**
     * 代理主机
     */
    private String host = "127.0.0.1";

    /**
     * 代理端口
     */
    private int port = 7897;
}
