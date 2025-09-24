package com.jay.aicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
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
import org.springframework.web.bind.annotation.*;
import com.jay.aicodemother.model.entity.App;
import com.jay.aicodemother.service.AppService;

import java.time.LocalDateTime;

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
        app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        
        return ResultUtils.success(app.getId());
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
    @PostMapping("/my/list/page")
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
    @PostMapping("/featured/list/page")
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
    @PostMapping("/admin/list/page")
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
    @GetMapping("/admin/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppByIdForAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        return ResultUtils.success(appService.getAppVO(app));
    }

    // endregion
}