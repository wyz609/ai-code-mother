/**
 * Class name: AuthCheck
 * Package: com.jay.aicodemother.annotation
 * Description:
 *
 * @Create: 2025/9/21 18:42
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    String mustRole() default "";

}