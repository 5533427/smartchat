package com.smartchat.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把各种异常统一转换为 {@link ApiResponse}
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：按 code 返回，HTTP 状态码统一 200（前端按 code 判断） */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.ok(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /** 参数校验失败（@Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return ResponseEntity.ok(ApiResponse.error(400, message));
    }

    /** 请求方法不支持（如用 POST 调 GET 接口） */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.ok(ApiResponse.error(405, "请求方法不支持：" + e.getMethod()));
    }

    /** 资源不存在（静态文件 404） */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, "资源不存在：" + e.getResourcePath()));
    }

    /** 未知异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误：" + e.getMessage()));
    }
}
