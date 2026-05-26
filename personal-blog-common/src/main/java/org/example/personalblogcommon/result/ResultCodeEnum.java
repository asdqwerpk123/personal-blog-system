package org.example.personalblogcommon.result;

import lombok.Getter;

/**
 * 统一响应码枚举，定义系统常用业务状态码和默认提示文案。
 * Controller、异常处理器和业务异常通过该枚举保持返回语义一致。
 */
@Getter
public enum ResultCodeEnum {
    /**
     * 请求处理成功。
     */
    SUCCESS(200, "操作成功"),
    /**
     * 通用业务失败。
     */
    FAIL(500, "操作失败"),
    /**
     * 请求参数不合法。
     */
    PARAM_ERROR(400, "请求参数错误"),
    /**
     * 目标数据不存在。
     */
    NOT_FOUND(404, "数据不存在"),
    /**
     * 未登录、令牌无效或权限不足。
     */
    UNAUTHORIZED(401, "未登录或无权限"),
    /**
     * 服务端未预期异常。
     */
    SYSTEM_ERROR(5000, "系统异常");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
