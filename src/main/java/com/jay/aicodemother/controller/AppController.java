package com.jay.aicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jay.aicodemother.ai.AiCodeGenTypeRoutingService;
import com.jay.aicodemother.annotation.AuthCheck;
import com.jay.aicodemother.common.BaseResponse;
import com.jay.aicodemother.common.DeleteRequest;
import com.jay.aicodemother.common.ResultUtils;
import com.jay.aicodemother.constant.AppConstant;
import com.jay.aicodemother.constant.UserConstant;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.exception.ThrowUtils;
import com.jay.aicodemother.model.dto.app.*;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import com.jay.aicodemother.model.vo.AppVO;
import com.jay.aicodemother.model.vo.ProjectFileVO;
import com.jay.aicodemother.service.ProjectDownloadService;
import com.jay.aicodemother.service.PreviewTokenService;
import com.jay.aicodemother.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import com.jay.aicodemother.model.entity.App;
import com.jay.aicodemother.service.AppService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 应用 控制层。
 *
 * @author <a href="https://github.com/wyz609">程序员阿阳</a>
 */
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
@Slf4j
@Validated  // 启用方法级参数验证
public class AppController {

    private static final long MAX_SOURCE_FILE_SIZE = 1024 * 1024;
    private static final long MAX_PROJECT_SOURCE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> SOURCE_FILE_EXTENSIONS = Set.of(
            "vue", "js", "jsx", "ts", "tsx", "css", "scss", "sass", "less",
            "html", "json", "md", "yaml", "yml", "txt"
    );
    private static final Set<String> EXCLUDED_SOURCE_DIRECTORIES = Set.of(
            "node_modules", "dist", "build", "target", ".git", ".idea", ".vscode", "coverage"
    );
    private final AppService appService;
    private final UserService userService;
    private final ProjectDownloadService projectDownloadService;
    private final AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;
    private final PreviewTokenService previewTokenService;

    // region 用户端接口

    /**
     * 创建应用
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@Valid @RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 校验参数
//        String appName = appAddRequest.getAppName(); // 唯一区别是该地方是使用用户传入的名称来指定应用名称
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.hasBlank(initPrompt), ErrorCode.PARAMS_ERROR, "应用名称和初始化提示不能为空");
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        app.setUserId(loginUser.getId());
        app.setPriority(0); // 默认优先级为0
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        // 展示设置为多文件生成类型
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功, ID:{}, 类型:{}", app.getId(), selectedCodeGenType.getValue());
        return ResultUtils.success(app.getId());
    }

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@Valid @RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }


    /**
     * 应用聊天生成代码 流式生成 SSE
     * @param appId 应用ID
     * @param message 用户信息
     * @param request 请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        log.info("用户开始生成代码，appId: {}, message: {}", appId, message);
        
        // 参数校验：检查应用ID是否有效（非空且大于0）
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 参数校验：检查用户消息是否为空或空白
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        
        // 获取当前登录用户信息，用于权限验证和日志记录
        User loginUser = userService.getLoginUser(request);
        
        try {
            // 调用服务层生成代码（流式），返回一个数据流
            // 使用较大的缓冲区并丢弃溢出数据，避免阻塞
            Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser)
                    // 使用更大的缓冲区（1000），并在溢出时丢弃最旧的数据
                    .onBackpressureBuffer(100);

            // 处理数据流，将每个数据块包装成SSE格式
            return contentFlux
                    .map(chunk -> {
                        // 将内容包装成 {"d": "内容"} 的JSON对象格式，符合统一响应结构体
                        Map<String, String> wrapper = Map.of("d", chunk);
                        String jsonData = JSONUtil.toJsonStr(wrapper);
                        // 构建SSE事件对象，包含数据部分
                        return ServerSentEvent.<String>builder()
                                .data(jsonData)
                                .build();
                    })
                    // SSE 响应已开始后不能再交给全局异常处理器写普通 HTTP 响应。
                    .onErrorResume(error -> {
                        log.error("代码生成过程中发生错误，appId: {}, userId: {}", appId, loginUser.getId(), error);
                        String errorMessage = error.getMessage();
                        if (StrUtil.isBlank(errorMessage)) {
                            errorMessage = "代码生成失败，请重试";
                        }
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("generation-error")
                                .data(JSONUtil.toJsonStr(Map.of("message", errorMessage)))
                                .build());
                    })
                    // 在数据流结束后发送一个"done"事件，通知客户端数据传输完成
                    .concatWith(Mono.just(
                            ServerSentEvent.<String>builder()
                                    .event("done")  // 自定义事件类型为"done"
                                    .data("")       // 空数据体
                                    .build()
                    ))
                    // 记录成功完成日志
                    .doOnComplete(() -> log.info("代码生成完成，appId: {}, userId: {}", appId, loginUser.getId()))
                    ;
        } catch (Exception e) {
            // 记录异常日志
            log.error("调用代码生成服务时发生异常，appId: {}, userId: {}, error: {}", 
                    appId, loginUser.getId(), e.getMessage(), e);
            // 构造错误响应
            Map<String, String> errorWrapper = Map.of("e", "代码生成过程中发生错误");
            String errorJsonData = JSONUtil.toJsonStr(errorWrapper);
            ServerSentEvent<String> errorEvent = ServerSentEvent.<String>builder()
                    .event("error")
                    .data(errorJsonData)
                    .build();
            
            // 返回错误事件并结束流
            return Flux.just(errorEvent)
                    .concatWith(Mono.just(
                            ServerSentEvent.<String>builder()
                                    .event("done")
                                    .data("")
                                    .build()
                    ));
        }
    }

    /**
     * Create a short-lived token for sandboxed preview resources. The generated application receives
     * access only to its own build files and never receives the user's authenticated session.
     */
    @GetMapping("/preview/token/{appId}")
    public BaseResponse<String> createPreviewToken(@PathVariable Long appId, HttpServletRequest request) {
        getOwnedApp(appId, request);
        return ResultUtils.success(previewTokenService.createToken(appId));
    }

    /**
     * Serve the generated application from a real URL so Vue bundle assets can be loaded by the iframe.
     */
    @GetMapping({"/preview/{appId}", "/preview/{appId}/", "/preview/{appId}/**"})
    public ResponseEntity<Resource> preview(@PathVariable Long appId,
                                             @RequestParam(required = false) Long after,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        String requestPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String previewPrefix = "/app/preview/" + appId;
        int prefixIndex = requestPath == null ? -1 : requestPath.indexOf(previewPrefix);
        String relativePath = prefixIndex < 0 ? "" : requestPath.substring(prefixIndex + previewPrefix.length());
        String requestedFile = relativePath.replaceFirst("^/+", "");

        boolean tokenAuthenticated = false;
        if (!requestedFile.isBlank()) {
            int separatorIndex = requestedFile.indexOf('/');
            String token = separatorIndex < 0 ? requestedFile : requestedFile.substring(0, separatorIndex);
            if (previewTokenService.isValidForApp(token, appId)) {
                tokenAuthenticated = true;
                requestedFile = separatorIndex < 0 ? "" : requestedFile.substring(separatorIndex + 1);
            }
        }
        if (!tokenAuthenticated) {
            User loginUser = userService.getLoginUser(request);
            appService.validateAppOwnership(app, loginUser.getId());
        }

        if (requestedFile.isBlank() && !request.getRequestURI().endsWith("/")) {
            return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                    .header(HttpHeaders.LOCATION, request.getRequestURI() + "/")
                    .build();
        }

        Path previewRoot = getProjectRoot(app).resolve(
                CodeGenTypeEnum.VUE_PROJECT.getValue().equals(app.getCodeGenType()) ? "dist" : ""
        ).toAbsolutePath().normalize();
        if (requestedFile.isBlank()) {
            requestedFile = "index.html";
        }
        Path file = previewRoot.resolve(requestedFile).normalize();
        if (!file.startsWith(previewRoot) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        try {
            if ("index.html".equals(requestedFile) && after != null
                    && Files.getLastModifiedTime(file).toMillis() < after) {
                return ResponseEntity.notFound().build();
            }
            MediaType mediaType = MediaType.parseMediaType(getContentType(file));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .contentType(mediaType)
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            log.error("读取预览资源失败，appId: {}, file: {}", appId, file, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Return source files from the generated project for the code viewer.
     */
    @GetMapping("/project/files/{appId}")
    public BaseResponse<List<ProjectFileVO>> listProjectFiles(@PathVariable Long appId,
                                                               HttpServletRequest request) {
        App app = getOwnedApp(appId, request);
        Path projectRoot = getProjectRoot(app).toAbsolutePath().normalize();
        ThrowUtils.throwIf(!Files.isDirectory(projectRoot), ErrorCode.NOT_FOUND_ERROR, "项目代码尚未生成");

        List<ProjectFileVO> projectFiles = new ArrayList<>();
        long totalSize = 0;
        try {
            Path realProjectRoot = projectRoot.toRealPath();
            try (Stream<Path> paths = Files.walk(realProjectRoot)) {
                List<Path> sourceFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> isViewableSourceFile(realProjectRoot.relativize(path)))
                        .sorted(Comparator.comparing(path -> realProjectRoot.relativize(path).toString()))
                        .toList();

                for (Path sourceFile : sourceFiles) {
                    Path realFile = sourceFile.toRealPath();
                    if (!realFile.startsWith(realProjectRoot)) {
                        continue;
                    }
                    long fileSize = Files.size(realFile);
                    if (fileSize > MAX_SOURCE_FILE_SIZE || totalSize + fileSize > MAX_PROJECT_SOURCE_SIZE) {
                        continue;
                    }
                    String relativePath = realProjectRoot.relativize(realFile).toString().replace(File.separatorChar, '/');
                    projectFiles.add(new ProjectFileVO(
                            relativePath,
                            getLanguage(relativePath),
                            Files.readString(realFile, StandardCharsets.UTF_8)
                    ));
                    totalSize += fileSize;
                }
            }
        } catch (IOException e) {
            log.error("读取项目源码失败，appId: {}, projectRoot: {}", appId, projectRoot, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取项目源码失败");
        }
        return ResultUtils.success(projectFiles);
    }

    private App getOwnedApp(Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        appService.validateAppOwnership(app, loginUser.getId());
        return app;
    }

    private Path getProjectRoot(App app) {
        return Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, app.getCodeGenType() + "_" + app.getId());
    }

    private boolean isViewableSourceFile(Path relativePath) {
        if (relativePath.getNameCount() == 0) {
            return false;
        }
        for (int i = 0; i < relativePath.getNameCount() - 1; i++) {
            if (EXCLUDED_SOURCE_DIRECTORIES.contains(relativePath.getName(i).toString())) {
                return false;
            }
        }
        String fileName = relativePath.getFileName().toString();
        if ("package-lock.json".equals(fileName)) {
            return false;
        }
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex >= 0
                && SOURCE_FILE_EXTENSIONS.contains(fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT));
    }

    private String getLanguage(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0) {
            return "text";
        }
        String extension = fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "vue" -> "html";
            case "jsx", "tsx" -> "javascript";
            case "yml" -> "yaml";
            default -> extension;
        };
    }

    private String getContentType(Path file) throws IOException {
        String detectedType = Files.probeContentType(file);
        if (detectedType != null) {
            return detectedType;
        }
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".js") || fileName.endsWith(".mjs")) return "application/javascript;charset=UTF-8";
        if (fileName.endsWith(".css")) return "text/css;charset=UTF-8";
        if (fileName.endsWith(".html")) return "text/html;charset=UTF-8";
        if (fileName.endsWith(".json")) return "application/json;charset=UTF-8";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /**
     * 下载应用代码
     * @param appId 应用ID
     * @param response 响应
     * @param request 请求
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId, HttpServletResponse response, HttpServletRequest request){
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验， 只有应用创建者才能进行下载代码
        User loginUser = userService.getLoginUser(request);
        if(!app.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径(生成目录， 非部署目录)
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(), ErrorCode.NOT_FOUND_ERROR, "代码目录不存在,请先生成代码");
        // 6. 生成下载文件名(不建议添加中文内容)
        String downloadFileName = String.valueOf(appId);
        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }



    /**
     * 更新自己的应用（仅支持更新应用名称）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateMyApp(@Valid @RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(appUpdateRequest.getId() == null || appUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 获取应用信息并校验权限
        App app = appService.getById(appUpdateRequest.getId());
        appService.validateAppOwnership(app, loginUser.getId());
        
        // 只能更新应用名称
        if (StrUtil.isNotBlank(appUpdateRequest.getAppName())) {
            app.setAppName(appUpdateRequest.getAppName());
        }
        app.setEditTime(LocalDateTime.now());
        app.setId(appUpdateRequest.getId());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除自己的应用
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMyApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 获取应用信息并校验权限
        App app = appService.getById(deleteRequest.getId());
        appService.validateAppOwnership(app, loginUser.getId());
        // 所有权已在上方校验通过，应用创建者可以删除自己的作品。
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppById(@RequestParam Long id, HttpServletRequest request) {
        log.info("AppId: {}", id);
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 只能查看自己的应用详情
        appService.validateAppOwnership(app, loginUser.getId());
        
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询自己的应用列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppsByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 设置查询条件：只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());

        // 限制每页最多20个
        if (appQueryRequest.getPageSize() > 20) {
            appQueryRequest.setPageSize(20);
        }

        Page<AppVO> appVOPage = appService.getAppVOPage(appQueryRequest);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页查询精选应用列表（无需登录）
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listFeaturedAppsByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 限制每页最多20个
        if (appQueryRequest.getPageSize() > 20) {
            appQueryRequest.setPageSize(20);
        }
        
        Page<AppVO> appVOPage = appService.getFeaturedAppVOPage(appQueryRequest);
        return ResultUtils.success(appVOPage);
    }

    // endregion

    // region 管理员接口

    /**
     * 管理员删除任意应用
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 判断待删除的数据是否存在
        App app = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 删除 应用
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新任意应用
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateApp(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        ThrowUtils.throwIf(appAdminUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(appAdminUpdateRequest.getId() == null || appAdminUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        
        App app = appService.getById(appAdminUpdateRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        // 更新允许的字段
        if (StrUtil.isNotBlank(appAdminUpdateRequest.getAppName())) {
            app.setAppName(appAdminUpdateRequest.getAppName());
        }
        if (StrUtil.isNotBlank(appAdminUpdateRequest.getCover())) {
            app.setCover(appAdminUpdateRequest.getCover());
        }
        if (appAdminUpdateRequest.getPriority() != null) {
            app.setPriority(appAdminUpdateRequest.getPriority());
        }
        // 设置
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppsByPageForAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 管理员查询不限制每页数量，但设置一个合理默认值
        if (appQueryRequest.getPageSize() <= 0) {
            appQueryRequest.setPageSize(10);
        }
        
        Page<AppVO> appVOPage = appService.getAppVOPage(appQueryRequest);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员根据 id 获取应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppByIdForAdmin(@RequestParam long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        return ResultUtils.success(appService.getAppVO(app));
    }


    // endregion
}
