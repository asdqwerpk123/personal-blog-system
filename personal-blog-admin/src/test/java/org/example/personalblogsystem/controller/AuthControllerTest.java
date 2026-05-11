package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.auth.JwtTokenService;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.service.PasswordHashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.personalblogsystem.testsupport.SecurityMockMvcSupport.secureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Autowired
    private PasswordHashService passwordHashService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = secureMockMvc(webApplicationContext);
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
        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getUserName()).isEqualTo("root");
        assertThat(principal.getRoleId()).isEqualTo(1L);
        assertThat(principal.getRoleCode()).isEqualTo("SUPER_ADMIN");

        Integer logCount = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where operator_user_id = 1
                          and target_type = 'AUTH'
                          and target_id = 1
                          and action_type = 'LOGIN_SUCCESS'
                          and action_result = 'SUCCESS'
                        """,
                Integer.class);
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    void shouldLoginAdminWithValidCredentials() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("admin_zhang", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userName").value("admin_zhang"))
                .andExpect(jsonPath("$.data.roleCode").value("ADMIN"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();
        AdminAuthPrincipal principal = jwtTokenService.parseAccessToken(token);
        assertThat(principal.getUserName()).isEqualTo("admin_zhang");
        assertThat(principal.getRoleCode()).isEqualTo("ADMIN");
    }

    @Test
    void shouldLoginNormalUserWithValidCredentials() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("jerry", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.userName").value("jerry"))
                .andExpect(jsonPath("$.data.roleCode").value("USER"))
                .andExpect(jsonPath("$.data.userStatus").value("ENABLED"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();
        AdminAuthPrincipal principal = jwtTokenService.parseAccessToken(token);
        assertThat(principal.getUserId()).isEqualTo(5L);
        assertThat(principal.getUserName()).isEqualTo("jerry");
        assertThat(principal.getRoleCode()).isEqualTo("USER");
    }

    @Test
    void shouldLoginNormalUserThroughUserAuthLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("jerry", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.userName").value("jerry"))
                .andExpect(jsonPath("$.data.roleCode").value("USER"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();
        AdminAuthPrincipal principal = jwtTokenService.parseAccessToken(token);
        assertThat(principal.getUserId()).isEqualTo(5L);
        assertThat(principal.getUserName()).isEqualTo("jerry");
        assertThat(principal.getRoleCode()).isEqualTo("USER");
    }

    @Test
    void shouldRejectAdminThroughUserAuthLoginWithoutReturningToken() throws Exception {
        mockMvc.perform(post("/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("admin_zhang", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
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
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        Integer logCount = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where operator_user_id = 1
                          and target_type = 'AUTH'
                          and target_id = 1
                          and action_type = 'LOGIN_FAILURE'
                          and action_result = 'FAILURE'
                        """,
                Integer.class);
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    void shouldRejectNormalUserAccessingAdminEndpointsAfterLogin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("jerry", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();

        mockMvc.perform(get("/admin/user/1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectDisabledUserOnLogin() throws Exception {
        jdbcTemplate.update("update sys_user set user_status = 'DISABLED' where user_name = ?", "root");

        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("root", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void shouldRejectDeletedUserOnLogin() throws Exception {
        jdbcTemplate.update("update sys_user set deleted = 1 where user_name = ?", "root");

        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("root", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void shouldRejectMissingUserOnLogin() throws Exception {
        jdbcTemplate.update("delete from sys_operation_log");

        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("missing-user", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

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

    @Test
    void shouldRegisterUserWithForcedUserRoleAndEnabledStatus() throws Exception {
        String userName = "writer_rose";
        String password = "123456";

        MvcResult result = mockMvc.perform(post("/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest(userName, password, "13800000066"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userName").value(userName))
                .andExpect(jsonPath("$.data.nickName").value("Writer Rose"))
                .andExpect(jsonPath("$.data.email").value("writer_rose@blog.local"))
                .andExpect(jsonPath("$.data.phone").value("13800000066"))
                .andExpect(jsonPath("$.data.roleCode").value("USER"))
                .andExpect(jsonPath("$.data.userStatus").value("ENABLED"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andReturn();

        Long userId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                select u.password_hash as passwordHash,
                       u.user_status as userStatus,
                       r.role_code as roleCode
                from sys_user u
                inner join sys_role r on r.id = u.role_id
                where u.user_name = ?
                """, userName);
        assertThat(row.get("roleCode")).isEqualTo("USER");
        assertThat(row.get("userStatus")).isEqualTo("ENABLED");
        assertThat(passwordHashService.matches(password, String.valueOf(row.get("passwordHash")))).isTrue();

        Map<String, Object> logRow = jdbcTemplate.queryForMap("""
                select operator_user_id as operatorUserId,
                       target_type as targetType,
                       target_id as targetId,
                       action_type as actionType,
                       action_result as actionResult
                from sys_operation_log
                where target_type = 'AUTH'
                  and target_id = ?
                  and action_type = 'REGISTER_USER'
                order by id desc
                limit 1
                """, userId);
        assertThat(logRow.get("operatorUserId")).isNull();
        assertThat(logRow.get("targetType")).isEqualTo("AUTH");
        assertThat(Long.parseLong(String.valueOf(logRow.get("targetId")))).isEqualTo(userId);
        assertThat(logRow.get("actionType")).isEqualTo("REGISTER_USER");
        assertThat(logRow.get("actionResult")).isEqualTo("SUCCESS");
    }

    @Test
    void shouldRejectDuplicateUsernameOnRegister() throws Exception {
        mockMvc.perform(post("/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("jerry", "123456", "13800000067"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void shouldRejectShortUserNameOnRegister() throws Exception {
        Map<String, Object> request = registerRequest("ab", "123456", "13800000069");

        mockMvc.perform(post("/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名长度必须为 3-20 位"));
    }

    @Test
    void shouldRejectTooLongUserNameOnRegister() throws Exception {
        Map<String, Object> request = registerRequest("writer_name_too_long_01", "123456", "13800000070");

        mockMvc.perform(post("/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名长度必须为 3-20 位"));
    }

    @Test
    void shouldRejectShortPasswordOnRegister() throws Exception {
        mockMvc.perform(post("/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("writer_short", "12345", "13800000068"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("密码长度必须为 6-20 位"));
    }

    @Test
    void shouldRegisterWithBlankOrOmittedOptionalEmailAndPhone() throws Exception {
        String userName = "writer_blank_opt";
        String password = "123456";
        Map<String, Object> request = registerRequest(userName, password, "13800000071");
        request.put("email", "   ");
        request.remove("phone");

        mockMvc.perform(post("/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userName").value(userName))
                .andExpect(jsonPath("$.data.email").isEmpty())
                .andExpect(jsonPath("$.data.phone").isEmpty())
                .andExpect(jsonPath("$.data.roleCode").value("USER"))
                .andExpect(jsonPath("$.data.userStatus").value("ENABLED"));
    }

    private LoginRequest loginRequest(String userName, String password) {
        LoginRequest request = new LoginRequest();
        request.setUserName(userName);
        request.setPassword(password);
        return request;
    }

    private Map<String, Object> registerRequest(String userName, String password, String phone) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("userName", userName);
        request.put("password", password);
        request.put("nickName", "Writer Rose");
        request.put("email", phone == null || phone.isBlank() ? "   " : userName + "@blog.local");
        request.put("phone", phone);
        request.put("avatarUrl", "https://example.com/user.png");
        request.put("introduction", "registered from test");
        request.put("roleId", 1);
        request.put("userStatus", "DISABLED");
        request.put("roleCode", "SUPER_ADMIN");
        return request;
    }
}
