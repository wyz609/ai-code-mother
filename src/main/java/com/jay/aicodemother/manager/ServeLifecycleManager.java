package com.jay.aicodemother.manager;

import com.jay.aicodemother.service.ServeDeployService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Serve 静态服务生命周期管理器。
 *
 * <p>跟随 Spring Boot 应用生命周期：应用启动完成后启动 Serve 静态服务
 * （用于本地托管已部署作品），应用关闭时自动停止。</p>
 */
@Component
public class ServeLifecycleManager {
    
    // 注入 Serve 部署服务
    @Autowired
    private ServeDeployService serveDeployService;
    
    /**
     * Spring Boot 启动完成后启动 Serve 服务
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 应用就绪后启动静态服务
        serveDeployService.startServeService();
    }
    
    /**
     * Spring Boot 关闭时停止 Serve 服务
     */
    @PreDestroy
    public void onApplicationShutdown() {
        // 打印关闭日志
        System.out.println("Shutting down Serve service...");
        // 停止静态服务
        serveDeployService.stopServeService();
    }
}
