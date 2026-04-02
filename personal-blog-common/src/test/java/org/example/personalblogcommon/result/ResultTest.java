package org.example.personalblogcommon.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultTest {

    @Test
    void shouldBuildSuccessResult() {
        Result<String> result = Result.ok("hello");

        assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode());
        assertEquals(ResultCodeEnum.SUCCESS.getMessage(), result.getMessage());
        assertEquals("hello", result.getData());
    }

    @Test
    void shouldBuildFailureResultFromEnum() {
        Result<Object> result = Result.fail(ResultCodeEnum.NOT_FOUND);

        assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), result.getCode());
        assertEquals(ResultCodeEnum.NOT_FOUND.getMessage(), result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void shouldBuildFailureResultFromCustomCodeAndMessage() {
        Result<Object> result = Result.fail(9999, "custom error");

        assertEquals(9999, result.getCode());
        assertEquals("custom error", result.getMessage());
        assertNull(result.getData());
    }
}