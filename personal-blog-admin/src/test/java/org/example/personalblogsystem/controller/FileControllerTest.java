package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.example.personalblogsystem.testsupport.SecurityMockMvcSupport.secureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = {
                "spring.profiles.active=test",
                "minio.enabled=true",
                "minio.endpoint=http://localhost:9000",
                "minio.access-key=minioadmin",
                "minio.secret-key=minioadmin",
                "minio.bucket-name=personal-blog"
        })
class FileControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = secureMockMvc(webApplicationContext);
    }

    @Test
    void shouldRejectAnonymousUpload() throws Exception {
        mockMvc.perform(multipart("/admin/files/upload").file(uploadFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectUserRoleUpload() throws Exception {
        String token = loginAndGetAccessToken("jerry", "123456");

        mockMvc.perform(multipart("/admin/files/upload")
                        .file(uploadFile())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectUnsafeMinioUploadTypeBeforeCallingMinio() throws Exception {
        String token = loginAndGetAccessToken("root", "123456");

        mockMvc.perform(multipart("/admin/files/upload")
                        .file(new MockMultipartFile("file", "payload.html", "text/html", "<script></script>".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不支持的图片格式"));
    }

    @Test
    void shouldRejectOversizedMinioUploadBeforeCallingMinio() throws Exception {
        String token = loginAndGetAccessToken("root", "123456");

        mockMvc.perform(multipart("/admin/files/upload")
                        .file(new MockMultipartFile("file", "cover.png", "image/png", new byte[10 * 1024 * 1024 + 1]))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("图片大小不能超过 10MB"));
    }

    private MockMultipartFile uploadFile() {
        return new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G'});
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
