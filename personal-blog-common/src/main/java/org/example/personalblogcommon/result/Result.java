package org.example.personalblogcommon.result;

import lombok.Data;

/**
 * 后端接口统一响应包装类，承载状态码、提示信息和业务数据。
 * 所有 Controller 和全局异常处理器通过该类型保持前端响应格式一致。
 */
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    /**
     * 创建成功响应。
     *
     * @param data 业务返回数据
     * @param <T> 数据类型
     * @return code 为 SUCCESS 的统一响应
     */
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCodeEnum.SUCCESS.getCode());
        result.setMessage(ResultCodeEnum.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 根据预定义响应码创建失败响应。
     *
     * @param codeEnum 失败响应码枚举
     * @param <T> 数据类型
     * @return 不包含 data 的失败响应
     */
    public static <T> Result<T> fail(ResultCodeEnum codeEnum) {
        Result<T> result = new Result<>();
        result.setCode(codeEnum.getCode());
        result.setMessage(codeEnum.getMessage());
        return result;
    }

    /**
     * 根据自定义响应码和消息创建失败响应。
     *
     * @param code 失败响应码
     * @param message 失败提示信息
     * @param <T> 数据类型
     * @return 不包含 data 的失败响应
     */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
