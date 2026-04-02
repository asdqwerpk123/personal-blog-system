package org.example.personalblogsystem.handler;

import org.example.personalblogcommon.handler.GlobalExceptionHandler;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnBusinessErrorResponse() throws Exception {
        mockMvc.perform(get("/test/exception"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCodeEnum.PARAM_ERROR.getMessage()));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/exception")
        public String throwException() {
            throw new org.example.personalblogcommon.exception.BlogException(ResultCodeEnum.PARAM_ERROR);
        }
    }
}