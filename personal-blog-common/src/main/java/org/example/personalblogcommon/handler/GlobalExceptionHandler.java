package org.example.personalblogcommon.handler;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BlogException.class)
    public Result<Object> handleBlogException(BlogException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException exception) {
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), "图片大小不能超过 10MB");
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception exception) {
        return Result.fail(ResultCodeEnum.SYSTEM_ERROR.getCode(), ResultCodeEnum.SYSTEM_ERROR.getMessage());
    }
}
