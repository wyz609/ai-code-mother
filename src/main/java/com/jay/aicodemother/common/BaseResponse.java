package com.jay.aicodemother.common;

import com.jay.aicodemother.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应类：所有接口返回的数据结构
 *
 * <p>约定：code=0 表示成功，非 0 表示失败；data 为业务数据；message 为提示信息。</p>
 *
 * @param <T> 数据类型
 */
@Data
public class BaseResponse<T> implements Serializable {

    // 状态码（0 成功，非 0 失败）
    private int code;

    // 业务数据
    private T data;

    // 提示信息
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
