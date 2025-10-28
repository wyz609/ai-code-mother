package com.jay.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import com.jay.aicodemother.model.vo.UserVO;
import com.jay.aicodemother.service.ChatHistoryService;
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
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    private final UserService userService;
    // 代码生成门面类
    private final AICodeGeneratorFacade aiCodeGeneratorFacade;

    private final ChatHistoryService historyService;

    private final ScreenshotService screenshotService;

    // 流式处理执行器
    private final StreamHandlerExecutor handlerExecutor;

    // Vue 项目构建器
    private final VueProjectBuilder vueProjectBuilder;

    // 创建一个线程池用于异步任务
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder()
                    .setNameFormat("screenshot-generator-%d")
                    .build()
    );

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }


    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用， 仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权限访问");
        }
        // 4. 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        //5. 通过校验后， 添加用户消息到对话历史
        historyService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());

        // 6. 调用 AI 生成代码
        Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 7. 使用流式处理器执行器进行处理流式响应结果
        return handlerExecutor.doExecute(contentFlux, historyService, appId, loginUser, codeGenTypeEnum);

    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        // 2. 获取应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 3. 验证用户是否有权限部署应用，仅本人可以进行部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权限部署该应用");
        }

        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();

        // 5. 生成 6 位deployKey  (大小写字母 + 数字)
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomStringUpper(6); // 使用大写字母和数字，提高可读性
        }

        // 6. 获取代码生成类型，构建原目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourcePath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        // 检查源目录是否存在
        File sourceDir = new File(sourcePath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "源目录不存在,请先生成代码");
        }

        // 7. Vue 项目特殊处理 ： 执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if(codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT){
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.builderProject(sourcePath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败,请重试");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDir, "dist");
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

            // 复制文件到部署目录
            FileUtil.copyContent(sourceDir, deployDir, true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败:" + e.getMessage());
        }

        // 9. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        updateApp.setId(appId);
        boolean updateResult = this.updateById(updateApp);
        // 检查更新是否成功
        ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "更新应用信息失败");

        // 10. 返回可访问的 URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    /**
     * 异步设置应用封面图片
     * @param appId
     * @param appDeployUrl
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appDeployUrl) {
        executorService.submit(() ->{
            try{
                String screenshotUrl = screenshotService.generateAndUploadScreenshot(appDeployUrl);
                App updateApp = new App();
                updateApp.setId(appId);
                updateApp.setCover(screenshotUrl);
                boolean updateResult = this.updateById(updateApp);
                // 检查更新是否成功
                ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "更新应用封面失败");
                log.info("异步生成应用封面完成，封面图片 URL ->{}", screenshotUrl);
            }catch (Exception e){
                log.error("异步生成应用截图并更新封面时发生异常：{}",e.getMessage(),e);
            }
        });
    }
    /**
     * 在组件销毁时关闭线程池
     */
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();

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

    @Override
    public Page<AppVO> getAppVOPage(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();

        // 限制分页大小
        if (pageSize > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量不能超过20个");
        }

        QueryWrapper queryWrapper = getQueryWrapper(appQueryRequest);
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为AppVO
        List<AppVO> appVOList = getAppVOList(appPage.getRecords());
        Page<AppVO> appVOPage = Page.of(pageNum, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(appVOList);

        return appVOPage;
    }

    @Override
    public Page<AppVO> getFeaturedAppVOPage(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();

        // 限制分页大小
        if (pageSize > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量不能超过20个");
        }
        // 进行查询精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = getQueryWrapper(appQueryRequest);
        // 添加精选条件：priority > 0
//        queryWrapper.gt("priority", 0);

        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为AppVO
        List<AppVO> appVOList = getAppVOList(appPage.getRecords());
        Page<AppVO> appVOPage = Page.of(pageNum, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(appVOList);

        return appVOPage;
    }

    @Override
    public void validateAppOwnership(App app, Long userId) {
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作此应用");
        }
    }

    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }

        // 先删除关联的历史对话
        try {
            historyService.deleteByAppId(appId);
        }catch (Exception e){
            log.error("删除关联的历史对话失败:{}", e.getMessage());
        }
        // 删除应用
        return super.removeById(appId);
    }
}