package com.jay.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.model.dto.user.UserQueryRequest;
import com.jay.aicodemother.model.enums.UserRoleEnum;
import com.jay.aicodemother.model.vo.LoginUserVO;
import com.jay.aicodemother.model.vo.UserVO;
import com.jay.aicodemother.utils.PasswordUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.mapper.UserMapper;
import com.jay.aicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.jay.aicodemother.constant.UserConstant.USER_LOGIN_STATE;
import static com.mybatisflex.core.query.QueryMethods.column;

/**
 * 用户 服务层实现。
 *
 * @author <a href="https://github.com/wyz609">程序员阿阳</a>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // 定义常量：账号最短长度
    private static final int MIN_ACCOUNT_LENGTH = 4;
    // 定义常量：密码最短长度
    private static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * 用户注册核心逻辑
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数（非空、长度、两次密码一致）
        validateRegistrationParams(userAccount, userPassword, checkPassword);

        // 2. 检查账号是否重复
        checkDuplicateAccount(userAccount);

        // 3. 加密密码（不可逆散列，保证明文不落库）
        String encryptPassword = PasswordUtil.encrypt(userPassword);

        // 4. 插入数据：构建新用户实体，默认昵称"无名"、默认角色为普通用户
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userName("无名")
                .userRole(UserRoleEnum.USER.getValue())
                .build();

        // 执行保存
        boolean saveResult = this.save(user);
        // 保存失败说明数据库异常，抛出系统错误
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        // 返回新用户 ID
        return user.getId();
    }

    /**
     * 将用户实体转换为登录用户 VO（脱敏：只暴露安全字段）
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        // 空用户直接返回 null
        if (user == null) {
            return null;
        }
        // 创建 VO 对象
        LoginUserVO loginUserVO = new LoginUserVO();
        // 属性拷贝（VO 不包含 userPassword 等敏感字段，天然脱敏）
        BeanUtil.copyProperties(user, loginUserVO);
        // 返回脱敏后的登录用户信息
        return loginUserVO;
    }

    /**
     * 用户登录核心逻辑
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验：账号密码不能为空
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 账号长度至少 4 位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        // 密码长度至少 8 位
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密：对输入的密码加密后与库中密文比对
        String encryptPassword = PasswordUtil.encrypt(userPassword);
        // 查询用户是否存在（账号 + 密文同时匹配）
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        // 用户不存在（或密码错误）
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态：把用户实体写入会话
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 获得脱敏后的用户信息并返回
        return this.getLoginUserVO(user);
    }

    /**
     * 从会话中获取当前登录用户（每次请求都会重新查库，保证数据最新）
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 判断是否已经登录：从会话中取出登录态
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        // 会话中无用户或 ID 为空，说明未登录，抛出未登录异常
        if(currentUser == null || currentUser.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 2. 从数据库中查询最新用户信息
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);

        // 用户可能已被删除，此时视为未登录
        if(currentUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 返回最新用户实体
        return currentUser;
    }

    /**
     * 将用户实体转换为脱敏视图 VO
     */
    @Override
    public UserVO getUserVO(User user){
        // 空用户返回 null
        if (user == null){
            return null;
        }
        // 创建 VO 并拷贝安全字段
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        // 返回脱敏 VO
        return userVO;
    }

    /**
     * 批量将用户实体列表转换为脱敏 VO 列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList){
        // 空列表直接返回空集合
        if(CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        // 逐个转换并收集为列表
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 根据查询请求构建 MyBatis-Flex 查询条件（用于用户分页查询）
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        // 请求体为空则参数错误
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 取出各查询条件：精确匹配 ID
        Long id = userQueryRequest.getId();
        // 账号模糊查询
        String userAccount = userQueryRequest.getUserAccount();
        // 昵称模糊查询
        String userName = userQueryRequest.getUserName();
        // 简介模糊查询
        String userProfile = userQueryRequest.getUserProfile();
        // 角色精确查询
        String userRole = userQueryRequest.getUserRole();
        // 排序字段
        String sortField = userQueryRequest.getSortField();
        // 排序方向（ascend/descend）
        String sortOrder = userQueryRequest.getSortOrder();
        // 组装查询条件并返回
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }



    /**
     * 校验注册参数
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     */
    private void validateRegistrationParams(String userAccount, String userPassword, String checkPassword) {
        // 任一参数为空则报错
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 账号长度不足最小要求
        if (userAccount.length() < MIN_ACCOUNT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        // 密码或确认密码长度不足
        if (userPassword.length() < MIN_PASSWORD_LENGTH || checkPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 两次输入密码不一致
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
    }

    /**
     * 检查账号是否重复
     *
     * @param userAccount 用户账号
     */
    private void checkDuplicateAccount(String userAccount) {
        // 按账号精确匹配统计记录数
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .where(column("userAccount").eq(userAccount));
        long count = this.mapper.selectCountByQuery(queryWrapper);
        // 已存在同账号则报错
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
    }
    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录：会话中无登录态则不可注销
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态（清空会话中的用户对象）
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        // 返回注销成功
        return true;
    }

}