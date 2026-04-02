package org.example.personalblogcommon.exception;

import org.example.personalblogcommon.result.ResultCodeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlogExceptionTest {

    @Test
    void shouldExposeEnumCodeAndMessage() {
        BlogException exception = new BlogException(ResultCodeEnum.PARAM_ERROR);

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals(ResultCodeEnum.PARAM_ERROR.getMessage(), exception.getMessage());
    }

    @Test
    void shouldExposeCustomCodeAndMessage() {
        BlogException exception = new BlogException(8888, "boom");

        assertEquals(8888, exception.getCode());
        assertEquals("boom", exception.getMessage());
    }
}