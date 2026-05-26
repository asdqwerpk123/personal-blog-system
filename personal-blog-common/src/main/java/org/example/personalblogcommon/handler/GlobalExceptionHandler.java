package org.example.personalblogcommon.handler;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，统一把业务异常、参数异常、上传异常和安全异常转换为 Result 响应。
 * 作为公共模块组件，为后台管理端和用户端接口提供一致的错误返回格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理系统主动抛出的业务异常。
     *
     * @param exception 携带业务错误码和提示信息的异常
     * @return 与业务错误码一致的失败响应
     */
    @ExceptionHandler(BlogException.class)
    public Result<Object> handleBlogException(BlogException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理参数校验或业务参数解析异常。
     *
     * @param exception 参数异常
     * @return 参数错误响应，消息保留异常中的具体原因
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException exception) {
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getMessage());
    }

    /**
     * 处理文件上传超过限制的异常。
     *
     * @param exception Spring MVC 上传大小超限异常
     * @return 参数错误响应，提示图片大小限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), "图片大小不能超过 10MB");
    }

    /**
     * 处理 Spring Security 认证失败异常。
     *
     * @param exception 认证异常
     * @return 未登录或认证失败响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Object> handleAuthenticationException(AuthenticationException exception) {
        return Result.fail(401, "未登录或认证失败，请重新登录");
    }

    /**
     * 处理 Spring Security 授权拒绝异常。
     *
     * @param exception 权限不足异常
     * @return 权限不足响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Object> handleAccessDeniedException(AccessDeniedException exception) {
        return Result.fail(403, "权限不足，无法访问该资源");
    }

    /**
     * 处理未被更具体处理器捕获的系统异常。
     *
     * @param exception 未知异常
     * @return 系统异常响应，避免向前端暴露堆栈细节
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception exception) {
        return Result.fail(ResultCodeEnum.SYSTEM_ERROR.getCode(), ResultCodeEnum.SYSTEM_ERROR.getMessage());
    }
}
