package com.jay.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jay.aicodemother.common.ResultUtils;
import com.jay.aicodemother.constant.AppConstant;
import com.jay.aicodemother.core.AICodeGeneratorFacade;
import com.jay.aicodemother.core.builder.VueProjectBuilder;
import com.jay.aicodemother.core.handler.StreamHandlerExecutor;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.exception.ThrowUtils;
import com.jay.aicodemother.model.dto.app.AppQueryRequest;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import com.jay.aicodemother.model.vo.AppVO;
import com.jay.aicodemother.model.vo.ProjectFileVO;
import com.jay.aicodemother.model.vo.UserVO;
import com.jay.aicodemother.service.ChatHistoryService;
import com.jay.aicodemother.service.PreviewTokenService;
import com.jay.aicodemother.service.ScreenshotService;
import com.jay.aicodemother.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.jay.aicodemother.model.entity.App;
import com.jay.aicodemother.mapper.AppMapper;
import com.jay.aicodemother.service.AppService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mybatisflex.core.query.QueryMethods.column;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/wyz609">程序员阿阳</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {


    private static final long MAX_SOURCE_FILE_SIZE = 1024 * 1024;
    private static final long MAX_PROJECT_SOURCE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> SOURCE_FILE_EXTENSIONS = Set.of(
            "vue", "js", "jsx", "ts", "tsx", "css", "scss", "sass", "less",
            "html", "json", "md", "yaml", "yml", "txt"
    );
    private static final Set<String> EXCLUDED_SOURCE_DIRECTORIES = Set.of(
            "node_modules", "dist", "build", "target", ".git", ".idea", ".vscode", "coverage"
    );

    // 用户服务：用于关联查询用户信息、权限校验
    private final UserService userService;
    // 代码生成门面类：统一调度不同代码生成类型（HTML/多文件/Vue 工程）
    private final AICodeGeneratorFacade aiCodeGeneratorFacade;

    // 对话历史服务：记录用户/AI 消息、删除关联历史
    private final ChatHistoryService historyService;

    // 截图服务：生成应用预览截图用于作品封面
    private final ScreenshotService screenshotService;

    // 预览令牌服务：为沙箱预览生成短期 token
    private final PreviewTokenService previewTokenService;

    // 流式处理执行器：根据生成类型选择对应流处理器
    private final StreamHandlerExecutor handlerExecutor;

    // Vue 项目构建器：执行 npm install / build 生成 dist
    private final VueProjectBuilder vueProjectBuilder;

    // 服务端口（默认 8123），用于拼接部署/预览 URL
    @Value("${server.port:8123}")
    private int serverPort;

    // 服务 context-path（默认空），用于拼接完整访问路径
    @Value("${server.servlet.context-path:}")
    private String serverContextPath;

    /**
     * 截图生成线程池
     * 核心线程数：2（截图任务不频繁，不需要太多线程）
     * 最大线程数：4
     * 使用守护线程，避免阻止 JVM 关闭
     */
    private final ExecutorService screenshotExecutor = Executors.newFixedThreadPool(
            2,
            new ThreadFactoryBuilder()
                    .setNameFormat("screenshot-generator-%d")
                    .setDaemon(true)  // 使用守护线程
                    .build()
    );

    /**
     * 在 Bean 销毁时关闭线程池
     */
    @PreDestroy
    public void destroy() {
        // 记录线程池关闭开始日志
        log.info("开始关闭截图生成线程池...");
        // 发起关闭请求，停止接收新任务
        screenshotExecutor.shutdown();
        try {
            // 等待最多 30 秒让正在执行的任务完成
            if (!screenshotExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("截图线程池未能在 30 秒内正常关闭，强制终止...");
                // 超时后强制中断所有任务
                screenshotExecutor.shutdownNow();
            }
            log.info("截图生成线程池已关闭");
        } catch (InterruptedException e) {
            log.warn("等待截图线程池关闭时被中断，强制终止...");
            // 等待过程被打断则强制关闭并恢复中断标志
            screenshotExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 将应用实体转换为带用户信息的 VO 视图
     */
    @Override
    public AppVO getAppVO(App app) {
        // 空应用返回 null
        if (app == null) {
            return null;
        }
        // 创建 VO 对象
        AppVO appVO = new AppVO();
        // 拷贝应用基础字段（不包含敏感字段）
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        // 有创建者时才关联查询
        if (userId != null) {
            // 查询创建者用户实体
            User user = userService.getById(userId);
            // 转换为脱敏用户 VO
            UserVO userVO = userService.getUserVO(user);
            // 挂载到应用 VO 上
            appVO.setUser(userVO);
        }
        // 返回带用户信息的应用 VO
        return appVO;
    }


    /**
     * 批量将应用实体列表转换为带用户信息的 VO 列表
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        // 空列表直接返回空集合
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        // 一次性查出所有相关用户并构建 id → VO 映射
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        // 逐个应用组装 VO 并挂载对应用户信息
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }


    /**
     * 判断相对路径对应的文件是否为"可查看的源文件"。
     *
     * <p>规则：① 不在 node_modules/dist 等排除目录内；② 文件名不是 package-lock.json；
     * ③ 扩展名属于允许的源码扩展名集合。</p>
     */
    private boolean isViewableSourceFile(Path relativePath) {
        // 空路径（根目录本身）不可查看
        if (relativePath.getNameCount() == 0) {
            return false;
        }
        // 检查所有父目录是否命中排除目录（排除 node_modules、dist、build 等）
        for (int i = 0; i < relativePath.getNameCount() - 1; i++) {
            if (EXCLUDED_SOURCE_DIRECTORIES.contains(relativePath.getName(i).toString())) {
                return false;
            }
        }
        // 取文件名
        String fileName = relativePath.getFileName().toString();
        // package-lock.json 体积大且无查看价值，直接排除
        if ("package-lock.json".equals(fileName)) {
            return false;
        }
        // 取扩展名索引
        int extensionIndex = fileName.lastIndexOf('.');
        // 有扩展名且扩展名属于允许集合才可查看
        return extensionIndex >= 0
                && SOURCE_FILE_EXTENSIONS.contains(fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT));
    }


    /**
     * 根据文件名后缀推导前端展示用的语言类型。
     * vue 映射为 html，jsx/tsx 映射为 javascript，yml 映射为 yaml，其余保持原扩展名。
     */
    private String getLanguage(String fileName) {
        // 取扩展名索引
        int extensionIndex = fileName.lastIndexOf('.');
        // 无扩展名时返回 text
        if (extensionIndex < 0) {
            return "text";
        }
        // 提取小写扩展名
        String extension = fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        // 按扩展名映射语言类型
        return switch (extension) {
            case "vue" -> "html";
            case "jsx", "tsx" -> "javascript";
            case "yml" -> "yaml";
            default -> extension;
        };
    }

    /**
     * 查看代码文件
     *
     * @param projectRoot 代码文件目录
     *             * @return 返回能查看的代码文件
     */
    @Override
    public List<ProjectFileVO> getListProject(Path projectRoot, Long appId) {

        // 收集可查看的源文件列表
        List<ProjectFileVO> projectFiles = new ArrayList<>();
        // 累计已收集文件的总大小，用于总量限制
        long totalSize = 0;
        try {
            // 解析项目根目录的真实路径（解析符号链接）
            Path realProjectRoot = projectRoot.toRealPath();
            // 递归遍历项目目录下所有文件
            try (Stream<Path> paths = Files.walk(realProjectRoot)) {
                // 过滤出：普通文件 + 可查看源文件（排除构建目录/package-lock 等），并按相对路径排序
                List<Path> sourceFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> isViewableSourceFile(realProjectRoot.relativize(path)))
                        .sorted(Comparator.comparing(path -> realProjectRoot.relativize(path).toString()))
                        .toList();

                // 遍历每个候选源文件
                for (Path sourceFile : sourceFiles) {
                    // 解析真实路径，防止符号链接逃逸出项目根目录
                    Path realFile = sourceFile.toRealPath();
                    // 安全校验：文件必须位于项目根目录内，否则跳过
                    if (!realFile.startsWith(realProjectRoot)) {
                        continue;
                    }
                    // 获取文件大小
                    long fileSize = Files.size(realFile);
                    // 单文件超过 1MB 或累计超过 5MB 时跳过（防止返回超大响应）
                    if (fileSize > MAX_SOURCE_FILE_SIZE || totalSize + fileSize > MAX_PROJECT_SOURCE_SIZE) {
                        continue;
                    }
                    // 计算相对路径（使用正斜杠分隔，兼容前端展示）
                    String relativePath = realProjectRoot.relativize(realFile).toString().replace(File.separatorChar, '/');
                    // 构造 VO：相对路径 + 语言类型 + 文件内容（UTF-8 读取）
                    projectFiles.add(new ProjectFileVO(
                            relativePath,
                            getLanguage(relativePath),
                            Files.readString(realFile, StandardCharsets.UTF_8)
                    ));
                    // 累加已收集大小
                    totalSize += fileSize;
                }
            }
        } catch (IOException e) {
            // 遍历/读取过程异常则记录日志并抛出系统异常
            log.error("读取项目源码失败，appId: {}, projectRoot: {}", appId, projectRoot, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取项目源码失败");
        }
        // 返回源文件列表
        return projectFiles;
    }

    /**
     * 对话生成代码核心逻辑（流式）
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验：应用 ID 必须为正数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        // 用户消息不能为空
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        // 应用不存在则 404
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用， 仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权限访问");
        }
        // 4. 获取应用的代码生成类型（html/multi_file/vue_project）
        String codeGenType = app.getCodeGenType();
        // 将字符串转为枚举
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        // 未知类型则报错
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        //5. 通过校验后， 添加用户消息到对话历史（供前端回放）
        historyService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());

        // 记录生成开始时间（用于判断构建产物是否新鲜）
        long generationStartedAt = System.currentTimeMillis();
        // 6. 调用 AI 生成代码（流式返回）
        Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 7. 使用流式处理器执行器进行处理流式响应结果
        return handlerExecutor.doExecute(contentFlux, historyService, appId, loginUser, codeGenTypeEnum)
                // 生成完成后异步截取本地预览作为作品封面
                .doOnComplete(() -> generateGeneratedAppScreenshotAsync(appId, codeGenTypeEnum, generationStartedAt));

    }

    /**
     * 部署应用：将生成的项目构建产物复制到部署目录并返回访问 URL
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验：应用 ID 必须为正数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        // 登录用户不能为空
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        // 2. 获取应用信息
        App app = this.getById(appId);
        // 应用不存在则 404
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 3. 验证用户是否有权限部署应用，仅本人可以进行部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权限部署该应用");
        }

        // 4. 检查是否已有 deployKey（已部署过则复用，实现增量更新）
        String deployKey = app.getDeployKey();

        // 5. 生成 6 位deployKey  (大小写字母 + 数字)
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomStringUpper(6); // 使用大写字母和数字，提高可读性
        }

        // 6. 获取代码生成类型，构建原目录路径
        String codeGenType = app.getCodeGenType();
        // 源码目录命名规则：{类型}_{appId}
        String sourceDirName = codeGenType + "_" + appId;
        // 拼接源码目录完整路径
        String sourcePath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        // 检查源目录是否存在
        File sourceDir = new File(sourcePath);
        // 源码目录不存在说明尚未生成代码
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "源目录不存在,请先生成代码");
        }

        // 7. Vue 项目特殊处理 ： 执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if(codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT){
            // Vue 项目需要构建（npm install + npm run build）
            boolean buildSuccess = vueProjectBuilder.builderProject(sourcePath);
            // 构建失败直接抛异常
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败,请重试");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDir, "dist");
            // 构建产物目录必须存在
            ThrowUtils.throwIf(!distDir.exists() || !distDir.isDirectory(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建成功，但未能生成 dist 目录");
            // 构建完成后， 需要将构建后的文件复制到部署目录
            sourceDir = distDir;
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        File deployDir = new File(deployDirPath);

        try {
            // 如果部署目录已存在，先删除原有内容，确保部署的是最新版本
            if (deployDir.exists()) {
                FileUtil.clean(deployDir);
            }

            // 复制文件到部署目录（覆盖式）
            FileUtil.copyContent(sourceDir, deployDir, true);
        } catch (Exception e) {
            // 复制失败抛出部署异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败:" + e.getMessage());
        }

        // 9. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        // 记录部署密钥
        updateApp.setDeployKey(deployKey);
        // 记录部署时间
        updateApp.setDeployedTime(LocalDateTime.now());
        // 指定更新哪条记录
        updateApp.setId(appId);
        // 执行更新
        boolean updateResult = this.updateById(updateApp);
        // 检查更新是否成功
        ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "更新应用信息失败");

        // 10. 返回可访问的 URL（部署后的静态资源由 /api/static/{deployKey}/ 提供）
        String appDeployUrl = buildDeployUrl(deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    /**
     * 根据当前服务端口与 context-path 构建部署访问 URL。
     * 部署文件统一放在 CODE_DEPLOY_ROOT_DIR 下，由 StaticResourceController 的
     * /api/static/{deployKey}/ 路径对外提供访问，因此 URL 必须与静态资源映射保持一致。
     */
    private String buildDeployUrl(String deployKey) {
        // 取部署域名（本地环境为 http://localhost）
        String host = AppConstant.CODE_DEPLOY_HOST;
        // 去掉末尾斜杠，避免拼接出双斜杠
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }

        // 处理 context-path：为 null 则视为空串
        String contextPath = serverContextPath == null ? "" : serverContextPath.trim();
        // 非空且不以 / 开头时补上前导斜杠
        if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        // 去掉末尾连续斜杠
        while (contextPath.endsWith("/")) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }

        // 拼接完整部署 URL：host:port + contextPath + /static/{deployKey}/
        return String.format("%s:%d%s/static/%s/", host, serverPort, contextPath, deployKey);
    }

    /**
     * 异步设置应用封面图片
     * @param appId 应用ID
     * @param appDeployUrl 应用部署URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appDeployUrl) {
        // 提交到截图线程池异步执行，避免阻塞部署主流程
        screenshotExecutor.submit(() ->{
            try{
                // 执行截图生成并持久化封面
                generateAndPersistAppCover(appId, appDeployUrl);
            }catch (Exception e){
                // 截图失败仅记录日志，不影响主流程
                log.error("异步生成应用截图并更新封面时发生异常：{}", e.getMessage(), e);
            }
        });
    }

    /**
     * Code generation does not require deployment. Capture the authenticated local preview so the
     * work card immediately reflects the latest generated result.
     *
     * <p>业务说明：代码生成完成后无需部署，直接截取带 token 的本地预览作为作品封面，
     * 让作品卡片即时反映最新生成结果。</p>
     */
    private void generateGeneratedAppScreenshotAsync(Long appId, CodeGenTypeEnum codeGenType, long generationStartedAt) {
        // 生成沙箱预览 token（预览接口鉴权用，避免暴露用户会话）
        String token = previewTokenService.createToken(appId);
        // context-path 为空时用默认空串
        String contextPath = StrUtil.emptyToDefault(serverContextPath, "");
        // 拼接本地预览 URL（走带 token 的预览接口，可正常加载资源）
        String previewUrl = String.format("http://127.0.0.1:%d%s/app/preview/%d/%s/",
                serverPort, contextPath, appId, token);
        // 提交截图任务到线程池异步执行
        screenshotExecutor.submit(() -> {
            try {
                // 先等待构建产物就绪（最多 5 分钟），避免截到空白页
                if (!waitForPreviewReady(appId, codeGenType, generationStartedAt)) {
                    log.warn("生成项目预览未在规定时间内就绪，跳过截图，appId: {}", appId);
                    return;
                }
                // 预览就绪后执行截图并更新封面
                generateAndPersistAppCover(appId, previewUrl);
            } catch (Exception e) {
                // 截图异常仅记录日志
                log.error("生成项目截图并更新封面时发生异常，appId: {}", appId, e);
            }
        });
    }

    /**
     * 轮询等待项目预览产物就绪。
     * Vue 工程要求 dist/index.html 的修改时间晚于生成开始时间；
     * 其余类型要求项目根目录存在 index.html。
     */
    private boolean waitForPreviewReady(Long appId, CodeGenTypeEnum codeGenType, long generationStartedAt) {
        // 计算项目根目录路径
        Path projectRoot = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, codeGenType.getValue() + "_" + appId);
        // 入口文件：Vue 工程为 dist/index.html，其余为 index.html
        Path entryFile = codeGenType == CodeGenTypeEnum.VUE_PROJECT
                ? projectRoot.resolve("dist").resolve("index.html")
                : projectRoot.resolve("index.html");
        // 轮询超时时间：5 分钟
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        // 循环轮询直到超时
        while (System.currentTimeMillis() < deadline) {
            try {
                // 入口文件存在，且（非 Vue 或文件修改时间晚于生成开始时间）视为构建就绪
                if (Files.isRegularFile(entryFile)
                        && (codeGenType != CodeGenTypeEnum.VUE_PROJECT
                        || Files.getLastModifiedTime(entryFile).toMillis() >= generationStartedAt)) {
                    return true;
                }
                // 未就绪则休眠 1 秒后重试
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // 被中断时恢复中断标志并返回失败
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                // 文件访问异常记录日志并返回失败
                log.warn("检查项目预览状态失败，appId: {}, 文件: {}", appId, entryFile, e);
                return false;
            }
        }
        // 超时仍未就绪返回 false
        return false;
    }

    /**
     * 生成网页截图并上传对象存储，然后更新应用封面字段
     */
    private void generateAndPersistAppCover(Long appId, String webUrl) {
        // 调用截图服务生成截图并上传，返回可访问的图片 URL
        String screenshotUrl = screenshotService.generateAndUploadScreenshot(webUrl);
        // 截图 URL 为空则抛操作异常
        ThrowUtils.throwIf(StrUtil.isBlank(screenshotUrl), ErrorCode.OPERATION_ERROR, "生成截图失败");
        // 构建更新对象：仅更新封面字段
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setCover(screenshotUrl);
        // 执行更新
        boolean updateResult = this.updateById(updateApp);
        // 更新失败则抛异常
        ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "更新应用封面失败");
        // 记录封面生成完成日志
        log.info("异步生成应用封面完成，appId: {}, 封面图片 URL -> {}", appId, screenshotUrl);
    }

    // 移除原来的 shutdown 方法，由 @PreDestroy destroy() 替代


    /**
     * 根据查询请求构建应用查询条件（用于分页查询）
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        // 请求体为空则参数错误
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 取出各查询条件：ID 精确匹配
        Long id = appQueryRequest.getId();
        // 应用名称模糊查询
        String appName = appQueryRequest.getAppName();
        // 生成类型精确查询
        String codeGenType = appQueryRequest.getCodeGenType();
        // 部署密钥精确查询
        String deployKey = appQueryRequest.getDeployKey();
        // 优先级精确查询
        Integer priority = appQueryRequest.getPriority();
        // 创建者 ID 精确查询
        Long userId = appQueryRequest.getUserId();
        // 排序字段
        String sortField = appQueryRequest.getSortField();
        // 排序方向（ascend/descend）
        String sortOrder = appQueryRequest.getSortOrder();
        // 封面图模糊查询
        String cover = appQueryRequest.getCover();
        // 初始化提示词模糊查询
        String initPrompt = appQueryRequest.getInitPrompt();

        // 组装查询条件并返回
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 应用分页查询（返回带用户信息的 VO）
     */
    @Override
    public Page<AppVO> getAppVOPage(AppQueryRequest appQueryRequest) {
        // 请求体为空则参数错误
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 取出页码
        long pageNum = appQueryRequest.getPageNum();
        // 取出每页大小
        long pageSize = appQueryRequest.getPageSize();

        // 限制分页大小（最多 20 条）
        if (pageSize > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量不能超过20个");
        }

        // 构建查询条件
        QueryWrapper queryWrapper = getQueryWrapper(appQueryRequest);
        // 执行分页查询应用实体
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为AppVO：批量关联用户信息
        List<AppVO> appVOList = getAppVOList(appPage.getRecords());
        // 构建同结构的 VO 分页对象（复用总记录数）
        Page<AppVO> appVOPage = Page.of(pageNum, pageSize, appPage.getTotalRow());
        // 写入 VO 记录列表
        appVOPage.setRecords(appVOList);

        return appVOPage;
    }

    /**
     * 精选应用分页查询（强制 priority = 精选优先级）
     */
    @Override
    public Page<AppVO> getFeaturedAppVOPage(AppQueryRequest appQueryRequest) {
        // 请求体为空则参数错误
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 取出页码
        long pageNum = appQueryRequest.getPageNum();
        // 取出每页大小
        long pageSize = appQueryRequest.getPageSize();

        // 限制分页大小
        if (pageSize > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量不能超过20个");
        }
        // 进行查询精选的应用：强制设置精选优先级作为查询条件
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        // 构建查询条件
        QueryWrapper queryWrapper = getQueryWrapper(appQueryRequest);
        // 添加精选条件：priority > 0（当前通过 setPriority 精确匹配，保留注释供参考）
//        queryWrapper.gt("priority", 0);

        // 执行分页查询
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为AppVO
        List<AppVO> appVOList = getAppVOList(appPage.getRecords());
        // 构建 VO 分页对象
        Page<AppVO> appVOPage = Page.of(pageNum, pageSize, appPage.getTotalRow());
        // 写入 VO 记录列表
        appVOPage.setRecords(appVOList);

        return appVOPage;
    }

    /**
     * 校验应用所有权：应用必须存在、用户必须登录且为应用创建者
     */
    @Override
    public void validateAppOwnership(App app, Long userId) {
        // 应用不存在则 404
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        // 用户未登录
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        // 非应用创建者则无权限
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作此应用");
        }
    }

    /**
     * 删除应用（重写）：先级联删除关联的对话历史，再删除应用本身
     */
    @Override
    public boolean removeById(Serializable id) {
        // ID 为空则返回失败
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        // ID 必须为正数
        if (appId <= 0) {
            return false;
        }

        // 先删除关联的历史对话（级联清理，避免脏数据）
        try {
            historyService.deleteByAppId(appId);
        }catch (Exception e){
            // 删除历史失败仅记录日志，不阻断应用删除
            log.error("删除关联的历史对话失败:{}", e.getMessage());
        }
        // 删除应用
        return super.removeById(appId);
    }
}
