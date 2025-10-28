package com.jay.aicodemother.service;

import com.jay.aicodemother.model.dto.app.AppQueryRequest;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.jay.aicodemother.model.entity.App;
import com.mybatisflex.core.paginate.Page;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/wyz609">程序员阿阳</a>
 */
public interface AppService extends IService<App> {

    /**
     * 获取脱敏的应用信息
     *
     * @param app 应用实体
     * @return 脱敏后的应用数据
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用封装列表
     *
     * @param appList 应用列表
     * @return 脱敏后的应用列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     *  生成代码
     * @param appId
     * @param message
     * @param loginUser
     */
     Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     * @param appId
     * @param loginUser
     * @return
     */
     String deployApp(Long appId, User loginUser);

    /**
     * 异步设置应用封面图片
     * @param appId
     * @param appDeployUrl
     */
    void generateAppScreenshotAsync(Long appId, String appDeployUrl);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 应用查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 分页获取应用封装列表
     *
     * @param appQueryRequest 应用查询请求
     * @return 分页的脱敏应用数据
     */
    Page<AppVO> getAppVOPage(AppQueryRequest appQueryRequest);

    /**
     * 分页获取精选应用封装列表（priority > 0）
     *
     * @param appQueryRequest 应用查询请求
     * @return 分页的精选应用数据
     */
    Page<AppVO> getFeaturedAppVOPage(AppQueryRequest appQueryRequest);

    /**
     * 校验应用权限（检查应用是否属于当前用户）
     *
     * @param app 应用实体
     * @param userId 当前用户ID
     */
    void validateAppOwnership(App app, Long userId);
}