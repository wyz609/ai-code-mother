package com.jay.aicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jay.aicodemother.annotation.AuthCheck;
import com.jay.aicodemother.common.BaseResponse;
import com.jay.aicodemother.common.DeleteRequest;
import com.jay.aicodemother.common.ResultUtils;
import com.jay.aicodemother.constant.UserConstant;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.exception.ThrowUtils;
import com.jay.aicodemother.model.dto.app.*;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import com.jay.aicodemother.model.vo.AppVO;
import com.jay.aicodemother.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import com.jay.aicodemother.model.entity.App;
import com.jay.aicodemother.service.AppService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 应用 控制层。
 *
 * @author <a href="https://github.com/wyz609">程序员阿阳</a>
 */
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
@Slf4j
public class AppController {

    private final AppService appService;
    private final UserService userService;

    // region 用户端接口

    /**
     * 创建应用
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
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
        // 展示设置为多文件生成类型
        app.setCodeGenType(CodeGenTypeEnum.VUE_PROJECT.getValue());
        
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        
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
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
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
            Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser)
                    // 添加背压处理，缓冲大小为32
                    .onBackpressureBuffer(32);
            
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
                    // 在数据流结束后发送一个"done"事件，通知客户端数据传输完成
                    .concatWith(Mono.just(
                            ServerSentEvent.<String>builder()
                                    .event("done")  // 自定义事件类型为"done"
                                    .data("")       // 空数据体
                                    .build()
                    ))
                    // 记录成功完成日志
                    .doOnComplete(() -> log.info("代码生成完成，appId: {}, userId: {}", appId, loginUser.getId()))
                    // 记录错误日志
                    .doOnError(error -> log.error("代码生成过程中发生错误，appId: {}, userId: {}, error: {}", 
                            appId, loginUser.getId(), error.getMessage(), error));
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
     * 更新自己的应用（仅支持更新应用名称）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateMyApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
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
        // 仅管理员可以进行删除
        if(!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppById(long id, HttpServletRequest request) {
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
    public BaseResponse<AppVO> getAppByIdForAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        return ResultUtils.success(appService.getAppVO(app));
    }


    // endregion
}