package org.example.personalblogcommon.exception;

import lombok.Getter;
import org.example.personalblogcommon.result.ResultCodeEnum;

@Getter
public class BlogException extends RuntimeException {
    private final Integer code;

    public BlogException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

    public BlogException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}