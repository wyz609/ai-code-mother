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

import javax.xml.transform.Result;
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


    private final AppService appService;
    private final UserService userService;
    private final ProjectDownloadService projectDownloadService;
    private final AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;
    private final PreviewTokenService previewTokenService;

    // region 用户端接口

    /**
     * 创建应用
     *
     * <p>核心业务：用户提交一段初始化提示词（描述想生成的网站），系统根据提示词
     * 自动路由到合适的代码生成类型（HTML/多文件/Vue 工程），创建一条应用记录。</p>
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@Valid @RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        // 校验请求体不能为空，否则直接抛出参数错误异常
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 校验参数
//        String appName = appAddRequest.getAppName(); // 唯一区别是该地方是使用用户传入的名称来指定应用名称
        // 取出用户输入的初始化提示词（这是应用的核心描述文本）
        String initPrompt = appAddRequest.getInitPrompt();
        // 初始化提示词不能为空，为空则抛出带提示信息的参数错误
        ThrowUtils.throwIf(StrUtil.hasBlank(initPrompt), ErrorCode.PARAMS_ERROR, "应用名称和初始化提示不能为空");
        
        // 获取当前登录用户（从会话中解析），用于关联应用归属
        User loginUser = userService.getLoginUser(request);
        
        // 创建应用实体对象
        App app = new App();
        // 将请求参数（initPrompt、codeGenType 等）批量拷贝到应用实体
//        BeanUtil.copyProperties(appAddRequest, app); // 在这里不进行拷贝生成类型，因为在下面已经进行了代码生成路由的选择进行设置
        app.setInitPrompt(initPrompt);
        // 应用名称直接截取提示词的前 12 个字符作为默认名称（超过部分丢弃）
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 设置应用归属用户为当前登录用户
        app.setUserId(loginUser.getId());
        // 默认优先级为 0（普通应用，非精选）x
        app.setPriority(0); // 默认优先级为0
        // 根据提示词内容智能路由出最合适的代码生成类型（html/multi_file/vue_project）
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        // 展示设置为多文件生成类型，将路由结果写入应用记录
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库，返回是否成功
        boolean result = appService.save(app);
        // 保存失败则抛出操作异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 记录应用创建成功的日志，包含应用 ID 与代码生成类型
        log.info("应用创建成功, ID:{}, 类型:{}", app.getId(), selectedCodeGenType.getValue());
        // 返回新创建应用的自增 ID 给前端，前端据此跳转到对话页
        return ResultUtils.success(app.getId());
    }

    /**
     * 应用部署
     *
     * <p>核心业务：将 AI 生成的项目构建产物部署为一个可公开访问的静态站点，
     * 返回访问 URL。</p>
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@Valid @RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        // 请求体不能为空
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        // 取出应用 ID
        Long appId = appDeployRequest.getAppId();
        // 应用 ID 必须为正数，否则抛出参数错误
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户，用于校验部署权限
        User loginUser = userService.getLoginUser(request);
        // 调用服务层执行部署，返回部署后的访问 URL
        String deployUrl = appService.deployApp(appId, loginUser);
        // 将部署 URL 返回给前端
        return ResultUtils.success(deployUrl);
    }


    /**
     * 应用聊天生成代码 流式生成 SSE
     *
     * <p>核心业务：用户发送修改/生成指令后，AI 通过 SSE 流式返回生成结果
     * （包括代码块、工具调用进度等），前端边接收边渲染。</p>
     *
     * @param appId 应用ID
     * @param message 用户信息
     * @param request 请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        // 记录用户开始生成代码的日志（应用 ID + 消息内容）
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
                        // 将包装后的 Map 序列化为 JSON 字符串
                        String jsonData = JSONUtil.toJsonStr(wrapper);
                        // 构建SSE事件对象，包含数据部分
                        return ServerSentEvent.<String>builder()
                                .data(jsonData)
                                .build();
                    })
                    // SSE 响应已开始后不能再交给全局异常处理器写普通 HTTP 响应。
                    // 因此在流内捕获异常并转换为 generation-error 事件发给前端
                    .onErrorResume(error -> {
                        // 记录代码生成异常日志
                        log.error("代码生成过程中发生错误，appId: {}, userId: {}", appId, loginUser.getId(), error);
                        // 提取异常消息；为空则使用默认错误文案
                        String errorMessage = error.getMessage();
                        if (StrUtil.isBlank(errorMessage)) {
                            errorMessage = "代码生成失败，请重试";
                        }
                        // 返回 generation-error 事件，携带错误消息 JSON
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
            // 构建 error 事件
            ServerSentEvent<String> errorEvent = ServerSentEvent.<String>builder()
                    .event("error")
                    .data(errorJsonData)
                    .build();
            
            // 返回错误事件并结束流（先发 error 再发 done，保证前端能收到终止信号）
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
     *
     * <p>业务说明：为沙箱预览生成一个短期令牌。生成的预览页面仅能访问自己的构建产物，
     * 不会携带用户会话信息，避免越权访问。</p>
     */
    @GetMapping("/preview/token/{appId}")
    public BaseResponse<String> createPreviewToken(@PathVariable Long appId, HttpServletRequest request) {
        // 校验应用归属：只有应用创建者才能为其生成预览令牌
        getOwnedApp(appId, request);
        // 调用令牌服务生成短期随机令牌（默认 10 分钟有效）并返回
        return ResultUtils.success(previewTokenService.createToken(appId));
    }

    /**
     * Serve the generated application from a real URL so Vue bundle assets can be loaded by the iframe.
     *
     * <p>业务说明：以真实 URL 提供生成应用的静态资源（前端 iframe 通过该接口加载
     * Vue 打包产物）。支持两种鉴权：带有效 token（沙箱）或登录用户本人访问。</p>
     */
    @GetMapping({"/preview/{appId}", "/preview/{appId}/", "/preview/{appId}/**"})
    public ResponseEntity<Resource> preview(@PathVariable Long appId,
                                             @RequestParam(required = false) Long after,
                                             HttpServletRequest request) {
        // 校验应用 ID 为正数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 查询应用记录
        App app = appService.getById(appId);
        // 应用不存在则抛出 404
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 从 Spring 的 HandlerMapping 属性中取出本次请求的完整内部路径
        String requestPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        // 定义预览 URL 前缀，用于剥离出相对路径部分
        String previewPrefix = "/app/preview/" + appId;
        // 找到前缀在完整路径中的起始位置
        int prefixIndex = requestPath == null ? -1 : requestPath.indexOf(previewPrefix);
        // 取出前缀之后的相对路径；找不到前缀则视为空路径
        String relativePath = prefixIndex < 0 ? "" : requestPath.substring(prefixIndex + previewPrefix.length());
        // 去掉路径开头的斜杠，得到最终请求的相对文件路径
        String requestedFile = relativePath.replaceFirst("^/+", "");

        // 标识是否已通过 token 完成鉴权
        boolean tokenAuthenticated = false;
        // 若非空路径，尝试从路径第一段解析出 token 进行沙箱鉴权
        if (!requestedFile.isBlank()) {
            // 路径第一段（遇 / 分隔）即 token
            int separatorIndex = requestedFile.indexOf('/');
            String token = separatorIndex < 0 ? requestedFile : requestedFile.substring(0, separatorIndex);
            // 校验 token 是否对该应用有效
            if (previewTokenService.isValidForApp(token, appId)) {
                tokenAuthenticated = true;
                // token 校验通过后，剥离 token 段，余下部分才是真实文件路径
                requestedFile = separatorIndex < 0 ? "" : requestedFile.substring(separatorIndex + 1);
            }
        }
        // 未通过 token 鉴权时，回退为登录用户本人访问（校验应用归属）
        if (!tokenAuthenticated) {
            // 获取登录用户
            User loginUser = userService.getLoginUser(request);
            // 校验该用户是否为应用所有者
            appService.validateAppOwnership(app, loginUser.getId());
        }

        // 请求根路径但 URL 结尾没有斜杠时，执行 308 永久重定向补上斜杠
        // （保证后续相对资源路径解析正确）
        if (requestedFile.isBlank() && !request.getRequestURI().endsWith("/")) {
            return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                    .header(HttpHeaders.LOCATION, request.getRequestURI() + "/")
                    .build();
        }

        // 计算预览根目录：Vue 工程指向 dist 打包产物目录，其余类型指向项目根目录
        Path previewRoot = getProjectRoot(app).resolve(
                CodeGenTypeEnum.VUE_PROJECT.getValue().equals(app.getCodeGenType()) ? "dist" : ""
        ).toAbsolutePath().normalize();
        // 空文件路径时默认访问入口文件 index.html
        if (requestedFile.isBlank()) {
            requestedFile = "index.html";
        }
        // 拼接出实际文件路径并做规范化（消除 .. 等）
        Path file = previewRoot.resolve(requestedFile).normalize();
        // 安全校验：解析后的文件必须位于预览根目录内，且是真实文件，否则 404
        if (!file.startsWith(previewRoot) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        try {
            // 轮询场景：请求 index.html 且带 after 时间戳时，若构建产物未在该时间后
            // 更新过，则返回 404 让前端继续轮询等待构建完成
            if ("index.html".equals(requestedFile) && after != null
                    && !hasProjectFileModifiedAfter(previewRoot, after)) {
                return ResponseEntity.notFound().build();
            }
            // 根据文件扩展名判定 Content-Type（JS/CSS/HTML 等）
            MediaType mediaType = MediaType.parseMediaType(getContentType(file));
            // 返回文件资源，禁用缓存并允许跨域访问
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .contentType(mediaType)
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            // 读取预览资源异常时记录日志并返回 500
            log.error("读取预览资源失败，appId: {}, file: {}", appId, file, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 判断预览根目录下是否存在修改时间晚于指定时间戳的文件。
     *
     * <p>用于构建轮询：Vue 工程异步构建完成后，dist 内文件修改时间会更新，
     * 据此判断构建是否已完成。</p>
     */
    private boolean hasProjectFileModifiedAfter(Path previewRoot, long timestamp) throws IOException {
        // 根目录不存在则直接返回 false（视为未构建完成）
        if (!Files.isDirectory(previewRoot)) {
            return false;
        }
        // 递归遍历根目录下所有文件
        try (Stream<Path> paths = Files.walk(previewRoot)) {
            // 只要存在一个普通文件的最后修改时间 >= 时间戳即返回 true
            return paths.filter(Files::isRegularFile)
                    .anyMatch(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() >= timestamp;
                        } catch (IOException e) {
                            // 单文件读取失败仅记录调试日志，不阻断整体判断
                            log.debug("读取预览文件修改时间失败: {}", path, e);
                            return false;
                        }
                    });
        }
    }

    /**
     * Return source files from the generated project for the code viewer.
     *
     * <p>业务说明：供前端"查看代码"面板使用。返回项目中所有可查看的源文件
     * （排除 node_modules/dist 等构建目录），附带语言类型与完整内容。</p>
     */
    @GetMapping("/project/files/{appId}")
    public BaseResponse<List<ProjectFileVO>> listProjectFiles(@PathVariable Long appId,
                                                               HttpServletRequest request) {
        // 获取当前登录用户拥有的应用（含所有权校验）
        App app = getOwnedApp(appId, request);
        // 计算项目根目录并规范化为绝对路径
        Path projectRoot = getProjectRoot(app).toAbsolutePath().normalize();
        // 项目目录不存在则说明代码尚未生成，抛出 404
        ThrowUtils.throwIf(!Files.isDirectory(projectRoot), ErrorCode.NOT_FOUND_ERROR, "项目代码尚未生成");

        List<ProjectFileVO> listProject = appService.getListProject(projectRoot,appId);

        return ResultUtils.success(listProject);


    }

    /**
     * 获取当前登录用户拥有的应用，并完成所有权校验。
     */
    private App getOwnedApp(Long appId, HttpServletRequest request) {
        // 应用 ID 必须为正数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 查询应用记录
        App app = appService.getById(appId);
        // 校验该应用是否属于当前用户（非所有者抛出无权限异常）
        appService.validateAppOwnership(app, loginUser.getId());
        // 返回应用实体
        return app;
    }

    /**
     * 根据应用的代码生成类型拼接其项目根目录路径。
     * 目录规则：{codeGenType}_{appId}，如 html_1、vue_project_2。
     */
    private Path getProjectRoot(App app) {
        return Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, app.getCodeGenType() + "_" + app.getId());
    }

    /**
     * 根据文件内容与扩展名推导 HTTP Content-Type。
     * 优先使用系统探测结果，探测失败时按扩展名手动判定。
     */
    private String getContentType(Path file) throws IOException {
        // 优先让系统根据文件内容探测 MIME 类型
        String detectedType = Files.probeContentType(file);
        // 探测成功则直接返回
        if (detectedType != null) {
            return detectedType;
        }
        // 探测失败：取小写文件名
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        // 按扩展名逐个判定：JS / MJS
        if (fileName.endsWith(".js") || fileName.endsWith(".mjs")) return "application/javascript;charset=UTF-8";
        // CSS
        if (fileName.endsWith(".css")) return "text/css;charset=UTF-8";
        // HTML
        if (fileName.endsWith(".html")) return "text/html;charset=UTF-8";
        // JSON
        if (fileName.endsWith(".json")) return "application/json;charset=UTF-8";
        // SVG 图片
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        // 兜底：一律按二进制流处理
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /**
     * 下载应用代码
     *
     * <p>业务说明：将应用生成的整个项目目录打包为 zip 并提供下载。</p>
     *
     * @param appId 应用ID
     * @param response 响应
     * @param request 请求
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId, HttpServletResponse response, HttpServletRequest request){
        // 1. 基础校验：应用 ID 必须为正数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        // 应用不存在则 404
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验， 只有应用创建者才能进行下载代码
        User loginUser = userService.getLoginUser(request);
        // 对比应用创建者与当前登录用户，不一致则抛无权限异常
        if(!app.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径(生成目录， 非部署目录)
        String codeGenType = app.getCodeGenType();
        // 目录命名规则与生成时一致：{类型}_{appId}
        String sourceDirName = codeGenType + "_" + appId;
        // 拼接出完整源码目录路径
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        // 目录不存在或不是文件夹则抛出 404，提示先生成代码
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(), ErrorCode.NOT_FOUND_ERROR, "代码目录不存在,请先生成代码");
        // 6. 生成下载文件名(不建议添加中文内容)
        String downloadFileName = String.valueOf(appId);
        // 7. 调用通用下载服务，将源码目录压缩为 zip 写入响应流
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }



    /**
     * 更新自己的应用（仅支持更新应用名称）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateMyApp(@Valid @RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        // 请求体不能为空
        ThrowUtils.throwIf(appUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        // 应用 ID 必须为正数
        ThrowUtils.throwIf(appUpdateRequest.getId() == null || appUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 获取应用信息并校验权限（非所有者抛出异常）
        App app = appService.getById(appUpdateRequest.getId());
        appService.validateAppOwnership(app, loginUser.getId());
        
        // 只能更新应用名称
        if (StrUtil.isNotBlank(appUpdateRequest.getAppName())) {
            app.setAppName(appUpdateRequest.getAppName());
        }
        // 更新编辑时间为当前时间
        app.setEditTime(LocalDateTime.now());
        // 设置要更新的记录 ID
        app.setId(appUpdateRequest.getId());
        // 执行更新操作
        boolean result = appService.updateById(app);
        // 更新失败则抛出操作异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回成功
        return ResultUtils.success(true);
    }

    /**
     * 删除自己的应用
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMyApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 请求体不能为空且 ID 必须为正数
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
        // 记录查询日志
        log.info("AppId: {}", id);
        // ID 必须为正数
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        
        // 查询应用记录
        App app = appService.getById(id);
        // 应用不存在则 404
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 只能查看自己的应用详情（校验所有权）
        appService.validateAppOwnership(app, loginUser.getId());
        
        // 转换为 VO（脱敏后的视图对象）并返回
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询自己的应用列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppsByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        // 请求体不能为空
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 设置查询条件：只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());

        // 限制每页最多20个（防止一次返回过多数据）
        if (appQueryRequest.getPageSize() > 20) {
            appQueryRequest.setPageSize(20);
        }

        // 执行分页查询并返回 VO 分页结果
        Page<AppVO> appVOPage = appService.getAppVOPage(appQueryRequest);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页查询精选应用列表（无需登录）
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listFeaturedAppsByPage(@RequestBody AppQueryRequest appQueryRequest) {
        // 请求体不能为空
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 限制每页最多20个
        if (appQueryRequest.getPageSize() > 20) {
            appQueryRequest.setPageSize(20);
        }
        
        // 查询精选应用分页（按优先级排序）
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
        // 请求体不能为空且 ID 必须为正数
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 判断待删除的数据是否存在
        App app = appService.getById(deleteRequest.getId());
        // 应用不存在则 404
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
        // 请求体不能为空
        ThrowUtils.throwIf(appAdminUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        // ID 必须为正数
        ThrowUtils.throwIf(appAdminUpdateRequest.getId() == null || appAdminUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        
        // 查询应用记录
        App app = appService.getById(appAdminUpdateRequest.getId());
        // 应用不存在则 404
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        // 更新允许的字段：应用名称非空才更新
        if (StrUtil.isNotBlank(appAdminUpdateRequest.getAppName())) {
            app.setAppName(appAdminUpdateRequest.getAppName());
        }
        // 封面图非空才更新
        if (StrUtil.isNotBlank(appAdminUpdateRequest.getCover())) {
            app.setCover(appAdminUpdateRequest.getCover());
        }
        // 优先级非空才更新（用于控制精选排序）
        if (appAdminUpdateRequest.getPriority() != null) {
            app.setPriority(appAdminUpdateRequest.getPriority());
        }
        // 设置编辑时间为当前时间
        app.setEditTime(LocalDateTime.now());
        // 执行更新
        boolean result = appService.updateById(app);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppsByPageForAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        // 请求体不能为空
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 管理员查询不限制每页数量，但设置一个合理默认值
        if (appQueryRequest.getPageSize() <= 0) {
            appQueryRequest.setPageSize(10);
        }
        
        // 执行分页查询
        Page<AppVO> appVOPage = appService.getAppVOPage(appQueryRequest);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员根据 id 获取应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppByIdForAdmin(@RequestParam long id) {
        // ID 必须为正数
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        
        // 查询应用记录
        App app = appService.getById(id);
        // 应用不存在则 404
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        // 转为 VO 返回（管理员可查看任意应用，无需归属校验）
        return ResultUtils.success(appService.getAppVO(app));
    }


    // endregion
}
