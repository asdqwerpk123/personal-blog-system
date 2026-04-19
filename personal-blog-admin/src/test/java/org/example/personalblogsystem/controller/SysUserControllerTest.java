package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
class SysUserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
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
                .andExpect(jsonPath("$.data.total").value(5));
    }

    @Test
    void shouldFilterPagedUsersByKeyword() throws Exception {
        mockMvc.perform(get("/admin/user/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "root"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].userName").value("root"));
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
