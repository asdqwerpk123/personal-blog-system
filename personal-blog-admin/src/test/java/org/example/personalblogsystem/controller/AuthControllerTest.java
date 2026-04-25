package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.auth.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class AuthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenService jwtTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        jdbcTemplate.update("delete from sys_operation_log");

        MvcResult result = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("root", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.userName").value("root"))
                .andExpect(jsonPath("$.data.nickName").value("Root"))
                .andExpect(jsonPath("$.data.roleId").value(1))
                .andExpect(jsonPath("$.data.roleCode").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.roleName").value("\u8D85\u7EA7\u7BA1\u7406\u5458"))
                .andExpect(jsonPath("$.data.userStatus").value("ENABLED"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();
        AdminAuthPrincipal principal = jwtTokenService.parseAccessToken(token);
        org.assertj.core.api.Assertions.assertThat(principal.getUserId()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(principal.getUserName()).isEqualTo("root");
        org.assertj.core.api.Assertions.assertThat(principal.getRoleId()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(principal.getRoleCode()).isEqualTo("SUPER_ADMIN");

        Integer logCount = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where operator_user_id = 1
                          and target_type = 'AUTH'
                          and target_id = 1
                          and action_type = 'LOGIN_SUCCESS'
                          and action_result = 'SUCCESS'
                          and action_detail like '%登录成功%'
                        """,
                Integer.class);
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    void shouldRejectBlankUsernameOnLogin() throws Exception {
        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("   ", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("userName must not be blank"));
    }

    @Test
    void shouldRejectBlankPasswordOnLogin() throws Exception {
        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("root", "   "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("password must not be blank"));
    }

    @Test
    void shouldRejectWrongPasswordOnLogin() throws Exception {
        jdbcTemplate.update("delete from sys_operation_log");

        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("root", "bad-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username or password is incorrect"));

        Integer logCount = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where operator_user_id = 1
                          and target_type = 'AUTH'
                          and target_id = 1
                          and action_type = 'LOGIN_FAILURE'
                          and action_result = 'FAILURE'
                          and action_detail like '%登录失败%'
                        """,
                Integer.class);
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    void shouldRejectNormalUserOnAdminLogin() throws Exception {
        jdbcTemplate.update("delete from sys_operation_log");

        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("jerry", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());

        Integer logCount = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where operator_user_id = 5
                          and target_type = 'AUTH'
                          and target_id = 5
                          and action_type = 'LOGIN_FAILURE'
                          and action_result = 'FAILURE'
                          and action_detail like '%登录失败%'
                        """,
                Integer.class);
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    void shouldRejectDisabledUserOnLogin() throws Exception {
        jdbcTemplate.update("update sys_user set user_status = 'DISABLED' where user_name = ?", "root");

        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("root", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username or password is incorrect"));
    }

    @Test
    void shouldRejectDeletedUserOnLogin() throws Exception {
        jdbcTemplate.update("update sys_user set deleted = 1 where user_name = ?", "root");

        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("root", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username or password is incorrect"));
    }

    @Test
    void shouldRejectMissingUserOnLogin() throws Exception {
        jdbcTemplate.update("delete from sys_operation_log");

        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("missing-user", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username or password is incorrect"));

        Integer logCount = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where target_type = 'AUTH'
                          and action_type = 'LOGIN_FAILURE'
                          and action_result = 'FAILURE'
                          and action_detail like '%missing-user%'
                        """,
                Integer.class);
        assertThat(logCount).isEqualTo(1);
    }

    private LoginRequest loginRequest(String userName, String password) {
        LoginRequest request = new LoginRequest();
        request.setUserName(userName);
        request.setPassword(password);
        return request;
    }
}
