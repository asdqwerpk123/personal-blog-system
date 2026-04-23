package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class SysOperationLogControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        seedOperationLogs();
    }

    @Test
    void shouldReturnPagedOperationLogs() throws Exception {
        mockMvc.perform(get("/admin/operation-log/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(4))
                .andExpect(jsonPath("$.data.records[0].targetType").value("AUTH"))
                .andExpect(jsonPath("$.data.records[0].actionType").value("LOGIN"))
                .andExpect(jsonPath("$.data.records[0].actionResult").value("SUCCESS"));
    }

    @Test
    void shouldFilterOperationLogs() throws Exception {
        mockMvc.perform(get("/admin/operation-log/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "10")
                        .param("targetType", "ARTICLE")
                        .param("actionResult", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].targetType").value("ARTICLE"))
                .andExpect(jsonPath("$.data.records[0].actionResult").value("SUCCESS"));
    }

    @Test
    void shouldRejectInvalidOperationLogPageCurrent() throws Exception {
        mockMvc.perform(get("/admin/operation-log/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectInvalidOperationLogPageSize() throws Exception {
        mockMvc.perform(get("/admin/operation-log/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectOperationLogPageSizeOverLimit() throws Exception {
        mockMvc.perform(get("/admin/operation-log/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectUnauthenticatedOperationLogReadEndpoints() throws Exception {
        mockMvc.perform(get("/admin/operation-log/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectInvalidActionResult() throws Exception {
        mockMvc.perform(get("/admin/operation-log/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "10")
                        .param("actionResult", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("actionResult must be one of SUCCESS, FAILED"));
    }

    @Test
    void shouldRejectBlankActionResult() throws Exception {
        mockMvc.perform(get("/admin/operation-log/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "10")
                        .param("actionResult", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("actionResult must be one of SUCCESS, FAILED"));
    }

    private void seedOperationLogs() {
        jdbcTemplate.update("delete from sys_operation_log");

        LocalDateTime baseTime = LocalDateTime.of(2026, 4, 14, 12, 0);
        insertOperationLog(1L, "ARTICLE", 101L, "CREATE", "SUCCESS", "Create article success", baseTime.minusMinutes(2));
        insertOperationLog(2L, "COMMENT", 202L, "UPDATE", "FAILED", "Update comment failed", baseTime.minusMinutes(1));
        insertOperationLog(1L, "TAG", 303L, "DELETE", "SUCCESS", "Delete tag success", baseTime);
    }

    private void insertOperationLog(Long operatorUserId,
                                    String targetType,
                                    Long targetId,
                                    String actionType,
                                    String actionResult,
                                    String actionDetail,
                                    LocalDateTime time) {
        Timestamp timestamp = Timestamp.valueOf(time);
        jdbcTemplate.update("""
                        insert into sys_operation_log (
                            operator_user_id, target_type, target_id, action_type, action_result, action_detail,
                            create_time, update_time, deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                operatorUserId,
                targetType,
                targetId,
                actionType,
                actionResult,
                actionDetail,
                timestamp,
                timestamp);
    }

    private String loginAndGetAccessToken(String userName, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(userName, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }

    private LoginRequest loginRequest(String userName, String password) {
        LoginRequest request = new LoginRequest();
        request.setUserName(userName);
        request.setPassword(password);
        return request;
    }
}
