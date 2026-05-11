package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.LinkedHashMap;

import static org.example.personalblogsystem.testsupport.SecurityMockMvcSupport.secureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class SysUserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = secureMockMvc(webApplicationContext);
    }

    @Test
    void shouldReturnWrappedUserResult() throws Exception {
        mockMvc.perform(get("/admin/user/1")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.userName").value("root"));
    }

    @Test
    void shouldReturnPagedUsers() throws Exception {
        mockMvc.perform(get("/admin/user/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(4));
    }

    @Test
    void shouldReturnOnlyNormalUsersForAdminPagedUsers() throws Exception {
        mockMvc.perform(get("/admin/user/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("admin_zhang", "123456"))
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].roleCode").value("USER"))
                .andExpect(jsonPath("$.data.records[1].roleCode").value("USER"));
    }

    @Test
    void shouldRejectAdminReadingAnotherAdmin() throws Exception {
        mockMvc.perform(get("/admin/user/2")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("admin_zhang", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限管理该用户"));
    }

    @Test
    void shouldFilterPagedUsersByKeyword() throws Exception {
        mockMvc.perform(get("/admin/user/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "tom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].userName").value("tom"));
    }

    @Test
    void shouldCreateUserWithDefaultEnabledStatus() throws Exception {
        mockMvc.perform(post("/admin/user")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userName", "new_admin",
                                "password", "123456",
                                "nickName", "New Admin",
                                "email", "new_admin@blog.local",
                                "phone", "13800000006",
                                "avatarUrl", "https://example.com/avatar.png",
                                "introduction", "created from test",
                                "roleId", 2
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userName").value("new_admin"))
                .andExpect(jsonPath("$.data.nickName").value("New Admin"))
                .andExpect(jsonPath("$.data.email").value("new_admin@blog.local"))
                .andExpect(jsonPath("$.data.phone").value("13800000006"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.data.introduction").value("created from test"))
                .andExpect(jsonPath("$.data.roleId").value(2))
                .andExpect(jsonPath("$.data.roleCode").value("ADMIN"))
                .andExpect(jsonPath("$.data.roleName").value("管理员"))
                .andExpect(jsonPath("$.data.userStatus").value("ENABLED"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void shouldRejectSuperAdminCreatingAnotherSuperAdmin() throws Exception {
        mockMvc.perform(post("/admin/user")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userName", "new_root",
                                "password", "123456",
                                "nickName", "New Root",
                                "email", "new_root@blog.local",
                                "phone", "13800000016",
                                "roleId", 1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限管理该用户"));
    }

    @Test
    void shouldRejectAdminCreatingAdminUser() throws Exception {
        mockMvc.perform(post("/admin/user")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("admin_zhang", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userName", "bad_admin",
                                "password", "123456",
                                "nickName", "Bad Admin",
                                "email", "bad_admin@blog.local",
                                "phone", "13800000026",
                                "roleId", 2
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限管理该用户"));
    }

    @Test
    void shouldRejectCurrentUserStatusAndPasswordManagement() throws Exception {
        String token = loginAndGetAccessToken("root", "123456");

        mockMvc.perform(put("/admin/user/1/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userStatus", "DISABLED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限管理该用户"));

        mockMvc.perform(put("/admin/user/1/password/reset")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newPassword", "654321"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限管理该用户"));
    }

    @Test
    void shouldReturnNotFoundForMissingUser() throws Exception {
        mockMvc.perform(get("/admin/user/999")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldRejectUnauthenticatedUserReadEndpoints() throws Exception {
        mockMvc.perform(get("/admin/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectNonPositiveCurrentPage() throws Exception {
        mockMvc.perform(get("/admin/user/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectNonPositivePageSize() throws Exception {
        mockMvc.perform(get("/admin/user/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectOversizedPageSize() throws Exception {
        mockMvc.perform(get("/admin/user/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
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
