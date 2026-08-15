package com.jay.aicodemother.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 20, message = "账号长度必须在 4-20 字符之间")
    private String userAccount;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度必须在 8-50 字符之间")
    private String userPassword;
}
