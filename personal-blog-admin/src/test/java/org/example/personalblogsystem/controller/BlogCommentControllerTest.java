package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.auth.JwtTokenService;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.entity.BlogComment;
import org.example.personalblogsystem.mapper.BlogCommentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.personalblogsystem.testsupport.SecurityMockMvcSupport.secureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class BlogCommentControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BlogCommentMapper blogCommentMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenService jwtTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = secureMockMvc(webApplicationContext);
    }

    @Test
    void shouldReturnPagedComments() throws Exception {
        mockMvc.perform(get("/admin/comment/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    void shouldFilterCommentsByKeywordStatusAndArticleId() throws Exception {
        jdbcTemplate.update("update blog_comment set comment_status = 'REJECTED' where id = 3");

        mockMvc.perform(get("/admin/comment/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "Vue")
                        .param("status", "REJECTED")
                        .param("articleId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(3))
                .andExpect(jsonPath("$.data.records[0].commentStatus").value("REJECTED"));
    }

    @Test
    void shouldReturnFlatCommentsForArticle() throws Exception {
        mockMvc.perform(get("/admin/comment/article/{articleId}", 1L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].parentId").doesNotExist())
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].parentId").value(1))
                .andExpect(jsonPath("$.data[1].replyToUserId").value(4));
    }

    @Test
    void shouldRejectInvalidCommentPageCurrent() throws Exception {
        mockMvc.perform(get("/admin/comment/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("current must be greater than 0"));
    }

    @Test
    void shouldRejectInvalidCommentPageSize() throws Exception {
        mockMvc.perform(get("/admin/comment/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("size must not exceed 100"));
    }

    @Test
    void shouldRejectInvalidCommentStatusFilter() throws Exception {
        mockMvc.perform(get("/admin/comment/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "10")
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("status must be one of PENDING, APPROVED, REJECTED"));
    }

    @Test
    void shouldRejectUnauthenticatedCommentWriteEndpoints() throws Exception {
        mockMvc.perform(put("/admin/comment/{id}/status", 1L)
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(delete("/admin/comment/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectInvalidCommentStatusUpdate() throws Exception {
        mockMvc.perform(put("/admin/comment/{id}/status", 1L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("status must be one of PENDING, APPROVED, REJECTED"));
    }

    @Test
    void shouldAcceptLowercaseCommentStatusInTurkishLocale() throws Exception {
        withLocale(new Locale("tr", "TR"), () -> mockMvc.perform(put("/admin/comment/{id}/status", 3L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("status", "approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.commentStatus").value("APPROVED")));
    }

    @Test
    void shouldUpdateCommentStatus() throws Exception {
        mockMvc.perform(put("/admin/comment/{id}/status", 3L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.commentStatus").value("REJECTED"));

        BlogComment updated = blogCommentMapper.selectById(3L);
        assertThat(updated).isNotNull();
        assertThat(updated.getCommentStatus()).isEqualTo("REJECTED");
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingComment() throws Exception {
        mockMvc.perform(put("/admin/comment/{id}/status", 999L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldRejectNormalUserCommentDeleteThroughAdminEndpoint() throws Exception {
        mockMvc.perform(delete("/admin/comment/{id}", 3L)
                        .header("Authorization", "Bearer " + issueNormalUserAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingComment() throws Exception {
        mockMvc.perform(delete("/admin/comment/{id}", 999L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldDeleteCommentWithSingleReadableOperationLog() throws Exception {
        mockMvc.perform(delete("/admin/comment/{id}", 3L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer deleted = jdbcTemplate.queryForObject(
                "select deleted from blog_comment where id = ?",
                Integer.class,
                3L);
        assertThat(deleted).isEqualTo(1);

        Integer readableDeleteLogs = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where target_type = 'COMMENT'
                          and target_id = ?
                          and action_type = 'DELETE_COMMENT'
                          and action_result = 'SUCCESS'
                          and action_detail = ?
                        """,
                Integer.class,
                3L,
                "删除评论：3");
        assertThat(readableDeleteLogs).isEqualTo(1);

        Integer legacyDeleteLogs = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where target_type = 'COMMENT'
                          and target_id = ?
                          and action_type = 'LOGIC_DELETE'
                        """,
                Integer.class,
                3L);
        assertThat(legacyDeleteLogs).isZero();
    }

    @Test
    void shouldRejectNormalUserDeletingOthersCommentThroughAdminEndpoint() throws Exception {
        mockMvc.perform(delete("/admin/comment/{id}", 1L)
                        .header("Authorization", "Bearer " + issueNormalUserAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
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

    private String issueNormalUserAccessToken() {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(5L, "jerry", 3L, "USER");
        return jwtTokenService.issueAccessToken(principal, jwtTokenService.calculateAccessTokenExpiresAt());
    }

    private JsonNode performJson(org.springframework.test.web.servlet.RequestBuilder requestBuilder) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void withLocale(Locale locale, ThrowingRunnable action) throws Exception {
        Locale previous = Locale.getDefault();
        Locale.setDefault(locale);
        try {
            action.run();
        } finally {
            Locale.setDefault(previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
