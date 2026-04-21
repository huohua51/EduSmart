package com.edusmart.server.common;

import lombok.Getter;

/**
 * 业务层错误，用于 Controller 层统一转换为 {@link ApiResponse}。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final int httpStatus;

    public BusinessException(String message) {
        this(-1, 400, message);
    }

    public BusinessException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, 401, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, 404, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(-1, 400, message);
    }
}
