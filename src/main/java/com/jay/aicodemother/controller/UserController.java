package com.jay.aicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import com.jay.aicodemother.annotation.AuthCheck;
import com.jay.aicodemother.common.BaseResponse;
import com.jay.aicodemother.common.DeleteRequest;
import com.jay.aicodemother.common.ResultUtils;
import com.jay.aicodemother.constant.UserConstant;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.exception.ThrowUtils;
import com.jay.aicodemother.model.dto.user.*;
import com.jay.aicodemother.model.vo.LoginUserVO;
import com.jay.aicodemother.model.vo.UserVO;
import com.jay.aicodemother.utils.PasswordUtil;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.service.UserService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 用户 控制层。
 *
 * @author <a href="https://github.com/wyz609">程序员阿阳</a>
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
@Validated  // 启用方法级参数验证
public class UserController {

    private final UserService userService;

    /**
     * 保存用户。
     *
     * @param user 用户
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody User user) {
        return userService.save(user);
    }

    /**
     * 根据主键删除用户。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return userService.removeById(id);
    }

    /**
     * 根据主键更新用户。
     *
     * @param user 用户
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody User user) {
        return userService.updateById(user);
    }

    /**
     * 查询所有用户。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<User> list() {
        return userService.list();
    }

    /**
     * 根据主键获取用户。
     *
     * @param id 用户主键
     * @return 用户详情
     */
    @GetMapping("getInfo/{id}")
    public User getInfo(@PathVariable Long id) {
        return userService.getById(id);
    }

    /**
     * 分页查询用户。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<User> page(Page<User> page) {
        return userService.page(page);
    }

/**
     * 用户注册。
     *
     * @param userRegisterRequest 用户注册请求体
     * @return 用户 id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@Valid @RequestBody UserRegisterRequest userRegisterRequest){
        // 请求体不能为空
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        // 取出注册账号
        String userAccount = userRegisterRequest.getUserAccount();
        // 取出注册密码
        String userPassword = userRegisterRequest.getUserPassword();
        // 取出确认密码（用于校验两次输入一致）
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 调用服务层执行注册（内部校验账号唯一性、密码长度、两次密码一致性等）
        Long result = userService.userRegister(userAccount, userPassword, checkPassword);
        // 返回新注册用户的 ID
        return ResultUtils.success(result);
    }

    /**
     * 用户登录。
     *
     * @param userLoginRequest 用户登录请求体
     * @param request          请求
     * @return 脱敏后的用户信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@Valid @RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 记录登录开始日志
        log.info("===========> 用户正在登录......");
        // 请求体不能为空
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        // 取出登录账号
        String userAccount = userLoginRequest.getUserAccount();
        // 取出登录密码
        String userPassword = userLoginRequest.getUserPassword();
        // 调用服务层完成登录（校验密码并写入会话），返回脱敏后的登录用户信息
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        // 记录登录成功日志
        log.info("===========> 用户登录成功......");
        // 返回登录用户信息
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        // 从会话中解析当前登录用户（未登录会抛出未登录异常）
        User loginUser = userService.getLoginUser(request);
        // 转为脱敏 VO 返回
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        // 调用服务层清除会话中的登录态
        boolean result = userService.userLogout(request);
        // 返回注销是否成功
        return ResultUtils.success(result);
    }


    /**
     * 创建用户（仅管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        // 请求体不能为空
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 创建用户实体
        User user = new User();
        // 将请求参数（账号、昵称、头像等）拷贝到实体
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        // 对默认密码进行加密存储
        String encryptPassword = PasswordUtil.encrypt(DEFAULT_PASSWORD);
        // 设置加密后的密码
        user.setUserPassword(encryptPassword);
        // 保存用户
        boolean result = userService.save(user);
        // 保存失败则抛出操作异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新用户 ID
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        // ID 必须为正数
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询用户
        User user = userService.getById(id);
        // 用户不存在则 404
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        // 返回用户实体（管理员可见完整信息）
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类（脱敏后的用户视图）
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        // 复用管理员查询接口获取用户实体
        BaseResponse<User> response = getUserById(id);
        // 取出用户实体
        User user = response.getData();
        // 转换为脱敏 VO（隐藏密码等敏感字段）并返回
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户（仅管理员）
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        // 请求体不能为空且 ID 必须为正数
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 执行删除
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户（仅管理员）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 请求体不能为空且 ID 不能为空
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 创建用户实体
        User user = new User();
        // 将请求参数（昵称、头像、简介等）拷贝到实体
        BeanUtil.copyProperties(userUpdateRequest, user);
        // 执行更新
        boolean result = userService.updateById(user);
        // 更新失败则抛出操作异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // 请求体不能为空
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 取出分页页码
        long pageNum = userQueryRequest.getPageNum();
        // 取出分页大小
        long pageSize = userQueryRequest.getPageSize();
        // 按查询条件（账号/昵称模糊等）分页查询用户实体
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏：构建同结构的分页 VO 对象，复用总数
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        // 将用户实体列表批量转换为脱敏 VO 列表
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        // 将 VO 列表写入分页结果
        userVOPage.setRecords(userVOList);
        // 返回脱敏后的分页结果
        return ResultUtils.success(userVOPage);
    }



}
