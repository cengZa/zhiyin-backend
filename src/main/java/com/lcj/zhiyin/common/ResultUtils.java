package com.lcj.zhiyin.common;

/**
 * 返回结果工具类
 */
public class ResultUtils {

    /**
     * 成功
     */
    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder()
                .code(200)
                .data(data)
                .message("success")
                .build();
    }

    /**
     * 成功（返回自定义消息）
     */
    public static <T> BaseResponse<T> success(T data, String message) {
        return BaseResponse.<T>builder()
                .code(200)
                .data(data)
                .message(message)
                .build();
    }

    /**
     * 失败返回 （默认错误信息）
     */
    public static BaseResponse<Void> error(ErrorCode errorCode) {
        return BaseResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }


    /**
     * 失败返回（自定义错误状态码和信息）
     */
    public static BaseResponse<Void> error(int code, String message) {
        return BaseResponse.<Void>builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 失败返回（带自定义消息）
     */
    public static BaseResponse<Void> error(ErrorCode errorCode, String message) {
        return BaseResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(message)
                .build();
    }

}
