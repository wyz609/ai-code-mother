package com.jay.aicodemother.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Class name: VueProjectBuilder
 * Package: com.jay.aicodemother.core.builder
 * Description: 该类用来执行 Vue 项目的构建过程
 *
 * 注意：该类使用单例模式管理线程池，确保整个应用共享一个线程池，
 * 避免创建多个线程池导致资源浪费。
 *
 * @Create: 2025/10/27 11:41
 * @Author: jay
 * @Version: 1.0
 */
@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * 构建任务线程池
     * 核心线程数：CPU 核心数，最大不超过 4
     * 使用守护线程，避免阻止 JVM 关闭
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.min(Runtime.getRuntime().availableProcessors(), 4),
            new ThreadFactoryBuilder()
                    .setNameFormat("vue-builder-%d")
                    .setDaemon(true)  // 使用守护线程，JVM 关闭时自动结束
                    .build()
    );

    /**
     * 在 Bean 销毁时关闭线程池
     * Spring 容器会自动调用此方法
     */
    @PreDestroy
    public void destroy() {
        log.info("开始关闭 Vue 项目构建线程池...");
        executor.shutdown();
        try {
            // 等待正在执行的任务完成，最多等待 30 秒
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("线程池未能在 30 秒内正常关闭，强制终止...");
                executor.shutdownNow();
                // 再等待 10 秒让线程响应中断
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("线程池强制终止后仍有任务未结束");
                }
            }
            log.info("Vue 项目构建线程池已关闭");
        } catch (InterruptedException e) {
            log.warn("等待线程池关闭时被中断，强制终止...");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 异步构建项目(不阻塞主线程)
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath){
        // 将构建任务提交到线程池
        executor.submit(() ->{
            try{
                // 异步执行构建
                builderProject(projectPath);
            }catch (Exception e){
                // 构建异常记录日志
                log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 构建 Vue 项目
     * @param projectPath
     * @return
     */
    public boolean builderProject(String projectPath){
        // 记录构建开始日志
        log.info("开始构建 Vue 项目: {}", projectPath);
        // 将路径转为文件对象
        File projectDir = new File(projectPath);
        // 校验项目目录存在且为目录
        if(!projectDir.exists() || !projectDir.isDirectory()){
            log.error("项目目录不存在或者不是目录: {}", projectPath);
            return false;
        }
        // 检查 package.json 文件是否存在
        File packageJson = new File(projectDir, "package.json");
        // 无 package.json 则无法构建
        if(!packageJson.exists()){
            log.error("项目目录下不存在 package.json 文件: {}", packageJson.getAbsolutePath());
            return false;
        }

        // 记录构建开始日志
        log.info("开始构建 Vue 项目: {}", projectPath);
        // 开始执行 npm install（安装依赖，5 分钟超时）
        if(!executeNpmInstall(projectDir)){
            log.error("npm install 失败");
            return false;
        }

        // 开始执行 npm run build（打包构建，3 分钟超时）
        if(!executeNpmBuild(projectDir)){
            log.error("npm run build 构建失败");
            return false;
        }

        // 验证 dist 目录是否存在
        File distDir = new File(projectDir, "dist");
        // 构建产物目录必须存在
        if(!distDir.exists()){
            log.error("项目构建完成，但未找到 dist 目录: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue 项目构建完成， dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }


    // 执行 npm install 命令（安装项目依赖）
    private boolean executeNpmInstall(File projectDir){
        // 记录安装日志
        log.info("执行 npm install....");
        // 拼接 npm install 命令
        String command = String.format("%s install", buildCommand());
        // 执行命令，设置五分钟超时时间
        return executeCommand(projectDir, command, 300); // 设置五分钟超时时间
    }

    // 执行 npm run build 命令（打包构建）
    private boolean executeNpmBuild(File projectDir){
        // 记录构建日志
        log.info("执行 npm build....");
        // 拼接 npm run build 命令
        String command = String.format("%s run build", buildCommand());
        // 执行命令，3 分钟超时
        return executeCommand(projectDir, command, 180); // 3 分钟超时
    }

    // 如果是 Window 系统，则需要在命令中添加 .cmd
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    // 根据操作系统生成 npm 命令（Windows 用 npm.cmd）
    private String buildCommand(){
        return isWindows() ? "npm" + ".cmd" : "npm";
    }

    /**
     * 执行命令
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            // 记录待执行命令
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            // 执行系统命令，指定工作目录
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 命令分割为数组
            );
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            // 超时未完成则强制终止
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            // 获取进程退出码
            int exitCode = process.exitValue();
            // 退出码为 0 表示成功
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                // 非 0 退出码表示失败
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            // 执行异常记录日志
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }


}