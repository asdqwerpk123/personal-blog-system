package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.personalblogsystem.testsupport.SecurityMockMvcSupport.secureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class AdminProfileControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = secureMockMvc(webApplicationContext);
    }

    @Test
    void shouldGetAndUpdateCurrentProfile() throws Exception {
        String token = loginAndGetAccessToken("root", "123456");

        mockMvc.perform(get("/admin/profile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userName").value("root"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mockMvc.perform(put("/admin/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickName", "Root Updated",
                                "email", "root-updated@blog.local",
                                "phone", "13800000009",
                                "avatarUrl", "/uploads/avatars/root.png",
                                "introduction", "更新简介"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickName").value("Root Updated"))
                .andExpect(jsonPath("$.data.avatarUrl").value("/uploads/avatars/root.png"))
                .andExpect(jsonPath("$.data.introduction").value("更新简介"));

        Integer logCount = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where operator_user_id = 1
                          and target_type = 'USER_PROFILE'
                          and target_id = 1
                          and action_type = 'UPDATE_PROFILE'
                          and action_result = 'SUCCESS'
                        """,
                Integer.class);
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    void shouldChangeCurrentPasswordAndRejectWrongOldPassword() throws Exception {
        String token = loginAndGetAccessToken("root", "123456");

        mockMvc.perform(put("/admin/profile/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "oldPassword", "wrong-password",
                                "newPassword", "654321"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("原密码错误"));

        mockMvc.perform(put("/admin/profile/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "oldPassword", "123456",
                                "newPassword", "654321"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer logCount = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where operator_user_id = 1
                          and target_type = 'USER_PROFILE'
                          and target_id = 1
                          and action_type = 'CHANGE_OWN_PASSWORD'
                          and action_result = 'SUCCESS'
                        """,
                Integer.class);
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    void shouldUploadAvatarUnderUploadsAvatars() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        MvcResult result = mockMvc.perform(multipart("/admin/files/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").isNotEmpty())
                .andReturn();

        String url = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("url")
                .asText();
        assertThat(url).startsWith("/uploads/avatars/");
        assertThat(url).endsWith(".png");
        Path storedFile = Path.of(url.substring(1));
        assertThat(Files.exists(storedFile)).isTrue();
        Files.deleteIfExists(storedFile);
    }

    private String loginAndGetAccessToken(String userName, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
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
