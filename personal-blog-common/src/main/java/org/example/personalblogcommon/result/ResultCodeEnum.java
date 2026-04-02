package org.example.personalblogcommon.result;

import lombok.Getter;

@Getter
public enum ResultCodeEnum {
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "请求参数错误"),
    NOT_FOUND(404, "数据不存在"),
    UNAUTHORIZED(401, "未登录或无权限"),
    SYSTEM_ERROR(5000, "系统异常");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}