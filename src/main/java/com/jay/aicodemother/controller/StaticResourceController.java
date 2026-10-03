package com.jay.aicodemother.controller;

import cn.hutool.core.util.StrUtil;
import com.jay.aicodemother.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/static")
public class StaticResourceController {

    /**
     * 部署应用根目录。应用部署时会被复制到 code_deploy/{deployKey}，静态访问必须读取该目录。
     */
    private static final Path DEPLOY_ROOT = Paths.get(AppConstant.CODE_DEPLOY_ROOT_DIR)
            .toAbsolutePath()
            .normalize();

    /**
     * deployKey 只允许字母、数字、下划线和中划线，避免路径拼接异常。
     */
    private static final String DEPLOY_KEY_PATTERN = "^[A-Za-z0-9_-]+$";

    /**
     * 提供静态资源访问，支持目录重定向
     * 访问格式：http://localhost:8123/api/static/{deployKey}[/{fileName}]
     *
     * <p>业务说明：用户部署作品后，通过该接口对外提供构建产物的静态访问。
     * 全链路包含 deployKey 格式校验、路径穿越防护、符号链接逃逸防护等安全措施。</p>
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            // 1. 校验 deployKey，只允许简单 token，避免路径穿越。
            //    若为空或包含非法字符（如 ../），直接返回 404
            if (StrUtil.isBlank(deployKey) || !deployKey.matches(DEPLOY_KEY_PATTERN)) {
                return ResponseEntity.notFound().build();
            }

            // 2. 从 handler mapping 中提取 deployKey 之后的相对路径。
            //    得到形如 "/xxx/index.html" 的原始资源路径
            String pathWithinHandler = (String) request.getAttribute(
                    HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            // 定义静态资源前缀，用于剥离 deployKey 部分
            String prefix = "/static/" + deployKey;
            // 请求路径必须以该前缀开头，否则视为非法请求
            if (pathWithinHandler == null || !pathWithinHandler.startsWith(prefix)) {
                return ResponseEntity.notFound().build();
            }
            // 剥离前缀后得到 deployKey 之后的资源路径
            String resourcePath = pathWithinHandler.substring(prefix.length());

            // 3. 目录访问（不带斜杠）时重定向到带斜杠的 URL，让浏览器正确解析相对资源。
            //    例如 /static/abc → /static/abc/
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }

            // 4. 目录首页默认返回 index.html。
            //    访问目录根（带斜杠）时，自动指向目录内的入口文件
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }

            // 5. 规范化路径并校验必须位于部署目录内，防止 ../、绝对路径和符号链接逃逸。
            //    去掉开头的斜杠得到相对路径
            String relativePath = resourcePath.startsWith("/")
                    ? resourcePath.substring(1)
                    : resourcePath;
            // 相对路径不能为空，且不允许反斜杠（Windows 路径）与 .. 上级目录符号
            if (relativePath.isEmpty() || relativePath.contains("\\") || relativePath.contains("..")) {
                return ResponseEntity.notFound().build();
            }

            // 部署根目录不存在则 404
            if (!Files.isDirectory(DEPLOY_ROOT)) {
                return ResponseEntity.notFound().build();
            }
            // 解析部署根目录的真实路径（解析符号链接）
            Path realDeployRoot = DEPLOY_ROOT.toRealPath();

            // 拼接 deployKey 对应的部署子目录并规范化
            Path deployDir = realDeployRoot.resolve(deployKey).normalize();
            // 安全校验：子目录必须位于部署根目录内，且确实存在
            if (!deployDir.startsWith(realDeployRoot) || !Files.isDirectory(deployDir)) {
                return ResponseEntity.notFound().build();
            }
            // 解析部署子目录真实路径
            Path realDeployDir = deployDir.toRealPath();
            // 防止子目录本身是符号链接指向部署根之外
            if (!realDeployDir.startsWith(realDeployRoot)) {
                return ResponseEntity.notFound().build();
            }

            // 拼接目标文件完整路径并规范化
            Path file = realDeployDir.resolve(relativePath).normalize();
            // 安全校验：文件必须位于部署子目录内且是普通文件
            if (!file.startsWith(realDeployDir) || !Files.isRegularFile(file)) {
                return ResponseEntity.notFound().build();
            }
            // 解析文件真实路径，二次防符号链接逃逸
            Path realFile = file.toRealPath();
            // 最终校验真实文件仍位于部署子目录内
            if (!realFile.startsWith(realDeployDir)) {
                return ResponseEntity.notFound().build();
            }

            // 6. 返回文件资源。
            Resource resource = new FileSystemResource(realFile);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(realFile.toString()))
                    .body(resource);
        } catch (IOException e) {
            // 文件系统 IO 异常统一按 404 处理（文件可能不存在或已被清理）
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // 其他未预期异常返回 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String filePath) {
        // HTML 页面
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        // CSS 样式
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        // JavaScript 脚本
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        // PNG 图片
        if (filePath.endsWith(".png")) return "image/png";
        // JPEG 图片
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        // 其他类型兜底为二进制流
        return "application/octet-stream";
    }
}
