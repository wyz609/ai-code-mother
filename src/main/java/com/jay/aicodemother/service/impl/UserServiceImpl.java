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

    // 定义常量
    private static final int MIN_ACCOUNT_LENGTH = 4;
    private static final int MIN_PASSWORD_LENGTH = 8;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        validateRegistrationParams(userAccount, userPassword, checkPassword);

        // 2. 检查账号是否重复
        checkDuplicateAccount(userAccount);

        // 3. 加密密码
        String encryptPassword = PasswordUtil.encrypt(userPassword);

        // 4. 插入数据
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userName("无名")
                .userRole(UserRoleEnum.USER.getValue())
                .build();

        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = PasswordUtil.encrypt(userPassword);
        // 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 获得脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 判断是否已经登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if(currentUser == null || currentUser.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 2. 从数据库中查询，
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);

        if(currentUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public UserVO getUserVO(User user){
        if (user == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList){
        if(CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
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
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < MIN_ACCOUNT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < MIN_PASSWORD_LENGTH || checkPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
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
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .where(column("userAccount").eq(userAccount));
        long count = this.mapper.selectCountByQuery(queryWrapper);
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
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

}