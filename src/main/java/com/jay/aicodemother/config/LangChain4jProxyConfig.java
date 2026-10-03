/**
 * Class name: LangChain4jProxyConfig
 * Package: com.jay.aicodemother.config
 * Description: 配置 LangChain4j 使用的 HTTP 代理
 *
 * @Create: 2026/7/22
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class LangChain4jProxyConfig {

    // 注入代理配置
    @Autowired
    private ProxyConfig proxyConfig;

    /**
     * 通过设置 JVM 系统属性来配置代理
     * 这样 JDK HttpClient 会自动使用代理
     */
    @PostConstruct
    public void configureProxy() {
        // 仅在启用代理时配置
        if (proxyConfig.isEnabled()) {
            // 取代理主机
            String proxyHost = proxyConfig.getHost();
            // 取代理端口
            int proxyPort = proxyConfig.getPort();

            // 设置 HTTP 代理（JVM 全局系统属性）
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", String.valueOf(proxyPort));

            // 设置 HTTPS 代理
            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", String.valueOf(proxyPort));

            // 不代理本地地址（本地请求直连）
            System.setProperty("http.nonProxyHosts", "localhost|127.0.0.1|*.local");

            // 记录代理配置日志
            log.info("已配置 HTTP/HTTPS 代理: {}:{}", proxyHost, proxyPort);
        } else {
            // 未启用代理
            log.info("代理未启用");
        }
    }
}