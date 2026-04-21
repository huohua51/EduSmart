package com.edusmart.server.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一响应体。
 *
 * <p>为了与 Android 端历史上对 CloudBase 云函数返回体的解析兼容，
 * 保持 <code>{code, message, data}</code> 的外层结构；<code>code = 0</code> 表示成功。</p>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public ApiResponse() {}

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(-1, message, null);
    }
}
