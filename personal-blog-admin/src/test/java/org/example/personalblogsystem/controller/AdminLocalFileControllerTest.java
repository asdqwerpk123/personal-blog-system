package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class AdminLocalFileControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldUploadArticleCoverUnderUploadsArticleCovers() throws Exception {
        JsonNode response = performUpload("/admin/files/article-cover",
                new MockMultipartFile("file", "cover.gif", "image/gif", new byte[]{'G', 'I', 'F'}));

        assertThat(response.path("code").asInt()).isEqualTo(200);
        String url = response.path("data").path("url").asText();
        assertThat(url).startsWith("/uploads/article-covers/");
        assertThat(url).endsWith(".gif");
        Path storedFile = Path.of(url.substring(1));
        assertThat(Files.exists(storedFile)).isTrue();
        Files.deleteIfExists(storedFile);
    }

    @Test
    void shouldUploadPngArticleCoverUnderUploadsArticleCovers() throws Exception {
        JsonNode response = performUpload("/admin/files/article-cover",
                new MockMultipartFile("file", "cover.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'}));

        assertThat(response.path("code").asInt()).isEqualTo(200);
        String url = response.path("data").path("url").asText();
        assertThat(url).startsWith("/uploads/article-covers/");
        assertThat(url).endsWith(".png");
        Files.deleteIfExists(Path.of(url.substring(1)));
    }

    @Test
    void shouldRejectUnsafeArticleCoverTypes() throws Exception {
        assertRejectsArticleCoverType("cover.html", "text/html", "<script></script>".getBytes());
        assertRejectsArticleCoverType("cover.js", "application/javascript", "alert(1)".getBytes());
        assertRejectsArticleCoverType("cover.svg", "image/svg+xml", "<svg/>".getBytes());
    }

    private void assertRejectsArticleCoverType(String fileName, String contentType, byte[] content) throws Exception {
        mockMvc.perform(multipart("/admin/files/article-cover")
                        .file(new MockMultipartFile("file", fileName, contentType, content))
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不支持的图片格式"));
    }

    @Test
    void shouldRejectEmptyArticleCover() throws Exception {
        mockMvc.perform(multipart("/admin/files/article-cover")
                        .file(new MockMultipartFile("file", "cover.png", "image/png", new byte[0]))
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请选择图片文件"));
    }

    @Test
    void shouldRejectOversizedArticleCover() throws Exception {
        mockMvc.perform(multipart("/admin/files/article-cover")
                        .file(new MockMultipartFile("file", "cover.png", "image/png", new byte[2 * 1024 * 1024 + 1]))
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("图片大小不能超过 2MB"));
    }

    @Test
    void shouldUploadGifAvatar() throws Exception {
        JsonNode response = performUpload("/admin/files/avatar",
                new MockMultipartFile("file", "avatar.gif", "image/gif", new byte[]{'G', 'I', 'F'}));

        assertThat(response.path("code").asInt()).isEqualTo(200);
        String url = response.path("data").path("url").asText();
        assertThat(url).startsWith("/uploads/avatars/");
        assertThat(url).endsWith(".gif");
        Files.deleteIfExists(Path.of(url.substring(1)));
    }

    private JsonNode performUpload(String path, MockMultipartFile file) throws Exception {
        MvcResult result = mockMvc.perform(multipart(path)
                        .file(file)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
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
