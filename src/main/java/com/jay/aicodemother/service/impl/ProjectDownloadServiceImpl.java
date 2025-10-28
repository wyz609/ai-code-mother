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
        // 基础校验
        ThrowUtils.throwIf(StrUtil.isBlank(projectPath), ErrorCode.PARAMS_ERROR,"项目路径不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(downloadFileName), ErrorCode.PARAMS_ERROR,"下载文件名不能为空");
        File projectDir = new File(projectPath);
        ThrowUtils.throwIf(!projectDir.exists(), ErrorCode.PARAMS_ERROR,"项目路径不存在");
        ThrowUtils.throwIf(!projectDir.isDirectory(), ErrorCode.PARAMS_ERROR,"项目路径不是目录");
        log.info("开始打包下载项目： {} -> {}.zip", projectPath, downloadFileName);
        // 设置 HTTP 响应头
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + downloadFileName + ".zip");
        // 定义文件过滤器
        FileFilter filter = file -> isPathAllowed(projectDir.toPath(), file.toPath());
        // 压缩
        try {
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8,false, filter, projectDir);
            log.info("打包下载项目成功：{} -> {}.zip", projectPath, downloadFileName);
        } catch (IOException e) {
            log.error("打包下载项目失败",e);
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
        // 获取相对路径
        Path relativePath = projectRoot.relativize(fullPath);
        // 检查路径中的每一部分
        for (Path path : relativePath) {
            String pathName = path.toString();
            // 检查是否在忽略名称列表中
            if(IGNORED_NAME.contains(pathName)){
                return false;
            }

            // 检查文件扩展名
            if(IGNORED_EXTENSION.stream().anyMatch(pathName::endsWith)){
                return false;
            }
        }
        return true;
    }

}