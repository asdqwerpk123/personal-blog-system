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

import static org.example.personalblogsystem.testsupport.SecurityMockMvcSupport.secureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class DashboardControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = secureMockMvc(webApplicationContext);
    }

    @Test
    void shouldReturnDashboardSummary() throws Exception {
        mockMvc.perform(get("/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.articleCount").value(3))
                .andExpect(jsonPath("$.data.categoryCount").value(3))
                .andExpect(jsonPath("$.data.tagCount").value(4))
                .andExpect(jsonPath("$.data.commentCount").value(3))
                .andExpect(jsonPath("$.data.pendingCommentCount").isNumber())
                .andExpect(jsonPath("$.data.friendLinkCount").isNumber())
                .andExpect(jsonPath("$.data.latestArticles").isArray())
                .andExpect(jsonPath("$.data.latestArticles[?(@.id == 1)].categoryName").value("Backend"))
                .andExpect(jsonPath("$.data.latestComments").isArray())
                .andExpect(jsonPath("$.data.latestComments[?(@.id == 1)].articleTitle")
                        .value("Build a Personal Blog with Spring Boot"))
                .andExpect(jsonPath("$.data.latestOperationLogs").isArray());
    }

    @Test
    void shouldRejectUnauthenticatedDashboardSummary() throws Exception {
        mockMvc.perform(get("/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
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
