package com.smartchat.common;

/**
 * 业务异常：controller 层抛出后由 GlobalExceptionHandler 统一转换为 ApiResponse
 */
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
