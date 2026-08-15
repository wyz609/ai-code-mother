package com.jay.aicodemother.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     * 限制：4-20字符，只允许字母、数字、下划线
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 20, message = "账号长度必须在 4-20 字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String userAccount;

    /**
     * 密码
     * 限制：8-50字符
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度必须在 8-50 字符之间")
    private String userPassword;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String checkPassword;
}
