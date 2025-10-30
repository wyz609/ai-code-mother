package com.jay.aicodemother.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
 * @Create: 2025/10/27 11:41
 * @Author: jay
 * @Version: 1.0
 */
@Slf4j
@Component
public class VueProjectBuilder {

    // 创建一个线程池用于异步任务
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder()
                    .setNameFormat("vue-builder-%d")
                    .build()
    );

    /**
     * 异步构建项目(不阻主线程)
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath){
        // 将构建任务提交到线程池
        executor.submit(() ->{
            try{
                builderProject(projectPath);
            }catch (Exception e){
                log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(),e);
            }
        });
    }

    public void shutdown(){
        executor.shutdown();
        try{
            if(!executor.awaitTermination(60, TimeUnit.SECONDS)){
                executor.shutdownNow();
            }
        }catch (InterruptedException e){
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            log.error("等待线程池关闭时发生异常: {}", e.getMessage(),e);
        }
    }


    /**
     * 构建 Vue 项目
     * @param projectPath
     * @return
     */
    public boolean builderProject(String projectPath){
        log.info("开始构建 Vue 项目: {}", projectPath);
        File projectDir = new File(projectPath);
        if(!projectDir.exists() || !projectDir.isDirectory()){
            log.error("项目目录不存在或者不是目录: {}", projectPath);
            return false;
        }
        // 检查 package.json 文件是否存在
        File packageJson = new File(projectDir, "package.json");
        if(!packageJson.exists()){
            log.error("项目目录下不存在 package.json 文件: {}", packageJson.getAbsolutePath());
            return false;
        }

        log.info("开始构建 Vue 项目: {}", projectPath);
        // 开始执行 npm install
        if(!executeNpmInstall(projectDir)){
            log.error("npm install 失败");
            return false;
        }

        // 开始执行 npm run build
        if(!executeNpmBuild(projectDir)){
            log.error("npm run build 构建失败");
            return false;
        }

        // 验证 dist 目录是否存在
        File distDir = new File(projectDir, "dist");
        if(!distDir.exists()){
            log.error("项目构建完成，但未找到 dist 目录: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue 项目构建完成， dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }


    // 执行 npm install 命令
    private boolean executeNpmInstall(File projectDir){
        log.info("执行 npm install....");
        String command = String.format("%s install", buildCommand());
        return executeCommand(projectDir, command, 300); // 设置五分钟超时时间
    }

    // 执行 npm run build 命令
    private boolean executeNpmBuild(File projectDir){
        log.info("执行 npm build....");
        String command = String.format("%s run build", buildCommand());
        return executeCommand(projectDir, command, 180); // 3 分钟超时
    }

    // 如果是 Window 系统，则需要在命令中添加 .cmd
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

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
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 命令分割为数组
            );
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }


}