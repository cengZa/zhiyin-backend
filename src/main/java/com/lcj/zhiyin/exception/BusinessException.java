package com.lcj.zhiyin.exception;

import com.lcj.zhiyin.common.ErrorCode;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 自定义异常类
 */
@Getter
public class BusinessException extends RuntimeException implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message, int code) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

}
