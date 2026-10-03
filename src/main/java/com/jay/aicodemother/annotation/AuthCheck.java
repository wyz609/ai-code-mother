package com.jay.aicodemother.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解。
 *
 * <p>标注在 Controller 方法上，配合 {@link com.jay.aicodemother.aop.AuthInterceptor}
 * 实现角色权限控制。例如 @AuthCheck(mustRole = "admin") 表示仅管理员可访问。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 访问该方法所需的角色（如 "admin"）；为空表示无需特定角色
     */
    String mustRole() default "";

}