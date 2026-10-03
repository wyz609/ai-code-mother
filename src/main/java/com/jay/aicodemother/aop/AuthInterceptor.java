/**
 * Class name: AuthInterceptor
 * Package: com.jay.aicodemother.aop
 * Description:
 *
 * @Create: 2025/9/21 18:44
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.aop;

import com.jay.aicodemother.annotation.AuthCheck;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.enums.UserRoleEnum;
import com.jay.aicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuthInterceptor {

    // 用户服务：用于获取当前登录用户
    private final UserService userService;

    /**
     * 环绕通知：拦截带 @AuthCheck 注解的方法并做角色权限校验
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 取出注解中要求的角色
        String mustRole = authCheck.mustRole();
        // 获取当前请求上下文
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        // 从请求上下文中取出 HttpServletRequest
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 将必须角色字符串转为枚举
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        // 不需要权限则直接放行
        if(mustRoleEnum == null){
            return joinPoint.proceed();
        }

        // 需要权限才能进行放行
        // 获取当前登录用户的 权限角色
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 没有权限（未知角色）则进行拦截
        if(userRoleEnum == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 需求必须得有管理员权限，如果用户没有管理员权限则直接拒绝放行
        if(UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 通过权限校验，进行放行
        return joinPoint.proceed();
    }

}