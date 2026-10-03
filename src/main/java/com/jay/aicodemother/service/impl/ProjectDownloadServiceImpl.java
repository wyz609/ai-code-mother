package com.jay.aicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.exception.ThrowUtils;
import com.jay.aicodemother.service.ProjectDownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

/**
 * Class name: ProjectDownloadServiceImpl
 * Package: com.jay.aicodemother.service.impl
 * Description: 实现对 生成的代码文件进行压缩
 *
 * @Create: 2025/10/27 19:08
 * @Author: jay
 * @Version: 1.0
 */
@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    /**
     * 定义需要忽略（过滤 ） 的文件和目录名称
     */
    private static final Set<String> IGNORED_NAME = Set.of(
            "node_modules",
            ".git",
            "dist",
            ".DS_Store",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode"
    );

    /**
     * 忽略的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSION = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    @Override
    public void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse  response){
        // 基础校验：项目路径不能为空
        ThrowUtils.throwIf(StrUtil.isBlank(projectPath), ErrorCode.PARAMS_ERROR,"项目路径不能为空");
        // 下载文件名不能为空
        ThrowUtils.throwIf(StrUtil.isBlank(downloadFileName), ErrorCode.PARAMS_ERROR,"下载文件名不能为空");
        // 将路径转换为 File 对象
        File projectDir = new File(projectPath);
        // 路径必须存在
        ThrowUtils.throwIf(!projectDir.exists(), ErrorCode.PARAMS_ERROR,"项目路径不存在");
        // 路径必须是目录
        ThrowUtils.throwIf(!projectDir.isDirectory(), ErrorCode.PARAMS_ERROR,"项目路径不是目录");
        // 记录打包开始日志
        log.info("开始打包下载项目： {} -> {}.zip", projectPath, downloadFileName);
        // 设置 HTTP 响应头：状态码 200
        response.setStatus(HttpServletResponse.SC_OK);
        // 响应类型为 zip 压缩包
        response.setContentType("application/zip");
        // 设置下载文件名（Content-Disposition 附件下载）
        response.setHeader("Content-Disposition", "attachment; filename=" + downloadFileName + ".zip");
        // 定义文件过滤器：排除 node_modules/dist 等无关内容，只打包源码
        FileFilter filter = file -> isPathAllowed(projectDir.toPath(), file.toPath());
        // 压缩
        try {
            // 将项目目录压缩为 zip 并写入响应输出流（UTF-8 编码，递归压缩）
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8,false, filter, projectDir);
            // 记录打包成功日志
            log.info("打包下载项目成功：{} -> {}.zip", projectPath, downloadFileName);
        } catch (IOException e) {
            // 打包失败记录错误日志
            log.error("打包下载项目失败",e);
            // 抛出系统异常通知前端
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "打包下载项目失败");
        }
    }

    /**
     * 校验路径是否允许包含在压缩包中
     * @param projectRoot
     * @param fullPath
     * @return
     */
    private boolean isPathAllowed(Path projectRoot, Path fullPath){
        // 获取相对路径（相对于项目根目录）
        Path relativePath = projectRoot.relativize(fullPath);
        // 检查路径中的每一部分
        for (Path path : relativePath) {
            // 取当前路径段的名称
            String pathName = path.toString();
            // 检查是否在忽略名称列表中（node_modules/.git/dist 等）
            if(IGNORED_NAME.contains(pathName)){
                return false;
            }

            // 检查文件扩展名（.log/.tmp/.cache 等）
            if(IGNORED_EXTENSION.stream().anyMatch(pathName::endsWith)){
                return false;
            }
        }
        // 路径所有部分均通过检查则允许打包
        return true;
    }

}