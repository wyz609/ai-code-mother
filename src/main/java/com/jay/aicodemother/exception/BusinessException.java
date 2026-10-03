package com.jay.aicodemother.exception;

import lombok.Getter;

/**
 * 自定义业务异常。
 *
 * <p>携带业务错误码，由 {@link GlobalExceptionHandler} 统一捕获并转换为标准响应；
 * 支持 SSE 场景下以事件流格式返回错误。</p>
 */
@Getter
public class BusinessException extends RuntimeException{

    /**
     * 业务错误码
     */
    private final int code;

    /**
     * 构造：自定义错误码 + 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造：使用错误码枚举（信息取枚举默认值）
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造：使用错误码枚举 + 自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
