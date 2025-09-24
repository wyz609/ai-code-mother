package com.jay.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jay.aicodemother.constant.AppConstant;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.model.dto.app.AppQueryRequest;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.vo.AppVO;
import com.jay.aicodemother.model.vo.UserVO;
import com.jay.aicodemother.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.jay.aicodemother.model.entity.App;
import com.jay.aicodemother.mapper.AppMapper;
import com.jay.aicodemother.service.AppService;
import com.mybatisflex.core.paginate.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mybatisflex.core.query.QueryMethods.column;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/wyz609">程序员阿阳</a>
 */
@Service
@RequiredArgsConstructor
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    private final UserService userService;

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
}