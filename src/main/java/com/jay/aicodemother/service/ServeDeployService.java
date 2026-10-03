package com.jay.aicodemother.service;

import com.jay.aicodemother.constant.AppConstant;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ServeDeployService {
    
    // 部署文件根目录（与部署目录常量一致）
    private static final String CODE_BASE_DIR = AppConstant.CODE_DEPLOY_ROOT_DIR;
    // Serve 静态服务端口
    private static final int SERVE_PORT = 3000;
    // 保存 serve 子进程引用，用于后续停止
    private static Process serveProcess;
    
    /**
     * 启动 Serve 服务
     *
     * <p>业务说明：使用 npx serve 将部署目录作为静态站点托管在 3000 端口，
     * 用于本地对外展示已部署的应用。进程已存在且存活时不做重复启动。</p>
     */
    public void startServeService() {
        try {
            // 仅当进程未启动或已死亡时才启动新进程
            if (serveProcess == null || !serveProcess.isAlive()) {
                // 获取操作系统类型，用于区分 Windows 与其他系统的命令名
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                // Windows 下使用 npx.cmd
                if (os.contains("win")) {
                    pb = new ProcessBuilder(
                        "npx.cmd", "serve", CODE_BASE_DIR, "-p", String.valueOf(SERVE_PORT)
                    );
                } else {
                    // 其他系统使用 npx
                    pb = new ProcessBuilder(
                        "npx", "serve", CODE_BASE_DIR, "-p", String.valueOf(SERVE_PORT)
                    );
                }
                // 合并子进程的标准输出与错误输出到同一流，便于观察日志
                pb.redirectErrorStream(true);
                // 启动子进程
                serveProcess = pb.start();
                // 打印启动成功提示
                System.out.println("Serve service started on port " + SERVE_PORT);
            }
        } catch (Exception e) {
            // 启动失败则抛出运行时异常
            throw new RuntimeException("Failed to start serve service", e);
        }
    }
    
    /**
     * 关闭 Serve 服务
     */
    public void stopServeService() {
        // 进程存在且存活时才需要停止
        if (serveProcess != null && serveProcess.isAlive()) {
            // 先温和地请求进程退出
            serveProcess.destroy();
            try {
                // 最多等待 5 秒让进程正常退出
                serveProcess.waitFor(5, TimeUnit.SECONDS);
                System.out.println("Serve service stopped");
            } catch (InterruptedException e) {
                // 等待被打断则强制终止进程
                serveProcess.destroyForcibly();
                System.out.println("Serve service force stopped");
            }
        }
    }
}
