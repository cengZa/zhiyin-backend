package com.lcj.zhiyin.common;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 */
@Data
@Builder
public class BaseResponse<T> implements Serializable {
    private int code;
    private T data;
    private String message;
}
