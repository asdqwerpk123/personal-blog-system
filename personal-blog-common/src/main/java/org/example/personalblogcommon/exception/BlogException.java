package org.example.personalblogcommon.exception;

import lombok.Getter;
import org.example.personalblogcommon.result.ResultCodeEnum;

/**
 * 博客系统业务异常，携带统一响应码和提示信息。
 * 由全局异常处理器转换为标准 Result 失败响应，避免业务层直接拼装错误返回。
 */
@Getter
public class BlogException extends RuntimeException {
    /**
     * 返回给前端的业务响应码。
     */
    private final Integer code;

    /**
     * 根据预定义响应码创建业务异常。
     *
     * @param resultCodeEnum 统一响应码枚举
     */
    public BlogException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

    /**
     * 根据自定义响应码和消息创建业务异常。
     *
     * @param code 业务响应码
     * @param message 错误提示信息
     */
    public BlogException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
