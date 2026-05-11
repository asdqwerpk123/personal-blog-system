package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.personalblogsystem.testsupport.SecurityMockMvcSupport.secureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class UserArticleControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BlogArticleMapper blogArticleMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = secureMockMvc(webApplicationContext);
    }

    @Test
    void shouldPageOnlyCurrentUserArticlesAndExposeDashboardSummary() throws Exception {
        String token = loginAndGetAccessToken("jerry", "123456");

        mockMvc.perform(get("/user/articles/page")
                        .header("Authorization", "Bearer " + token)
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(3))
                .andExpect(jsonPath("$.data.records[0].articleTitle").value("My First Draft"));

        mockMvc.perform(get("/user/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.articleCount").value(1))
                .andExpect(jsonPath("$.data.draftCount").value(1))
                .andExpect(jsonPath("$.data.publishedCount").value(0))
                .andExpect(jsonPath("$.data.privateCount").value(0))
                .andExpect(jsonPath("$.data.recentArticles[0].id").value(3));
    }

    @Test
    void shouldListCategoriesAndTagsForUserArticleForm() throws Exception {
        String token = loginAndGetAccessToken("jerry", "123456");

        mockMvc.perform(get("/user/categories/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].categoryName").value("Backend"));

        mockMvc.perform(get("/user/tags/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].tagName").value("SpringBoot"));
    }

    @Test
    void shouldCreateUpdatePublishAndDeleteOnlyOwnArticle() throws Exception {
        String token = loginAndGetAccessToken("jerry", "123456");
        Map<String, Object> createRequest = articleRequest("User article " + UUID.randomUUID(), "", "DRAFT");

        MvcResult createResult = mockMvc.perform(post("/user/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.authorId").value(5))
                .andExpect(jsonPath("$.data.articleStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.coverUrl").doesNotExist())
                .andReturn();

        long articleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
        assertThat(articleId).isPositive();
        assertThat(blogArticleMapper.selectById(articleId).getAuthorId()).isEqualTo(5L);

        Map<String, Object> updateRequest = articleRequest("Updated own article", "updated-own-" + UUID.randomUUID(), "PRIVATE");
        updateRequest.put("articleSummary", "Updated summary");

        mockMvc.perform(put("/user/articles/{id}", articleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.articleTitle").value("Updated own article"))
                .andExpect(jsonPath("$.data.articleStatus").value("PRIVATE"));

        mockMvc.perform(put("/user/articles/{id}/status", articleId)
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.articleStatus").value("PUBLISHED"));

        mockMvc.perform(delete("/user/articles/{id}", articleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer deleted = jdbcTemplate.queryForObject(
                "select deleted from blog_article where id = ?",
                Integer.class,
                articleId);
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    void shouldRejectUserOperatingOtherAuthorsArticle() throws Exception {
        String token = loginAndGetAccessToken("jerry", "123456");

        mockMvc.perform(get("/user/articles/{id}", 2L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限操作"));

        mockMvc.perform(put("/user/articles/{id}/status", 2L)
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限操作"));

        mockMvc.perform(delete("/user/articles/{id}", 2L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限操作"));
    }

    @Test
    void shouldCreatePendingCommentForCurrentUser() throws Exception {
        String token = loginAndGetAccessToken("jerry", "123456");

        mockMvc.perform(post("/user/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.articleId").value(1))
                .andExpect(jsonPath("$.data.userId").value(5))
                .andExpect(jsonPath("$.data.commentStatus").value("PENDING"));
    }

    @Test
    void shouldRejectCommentForDraftArticle() throws Exception {
        String token = loginAndGetAccessToken("jerry", "123456");

        mockMvc.perform(post("/user/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest(3L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文章暂不允许评论"));
    }

    @Test
    void shouldRejectCommentForPrivateArticle() throws Exception {
        jdbcTemplate.update("UPDATE blog_article SET article_status = 'PRIVATE' WHERE id = ?", 1L);
        String token = loginAndGetAccessToken("jerry", "123456");

        mockMvc.perform(post("/user/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文章暂不允许评论"));
    }

    @Test
    void shouldRejectCommentWhenArticleDisallowsComments() throws Exception {
        jdbcTemplate.update("UPDATE blog_article SET allow_comment = 0 WHERE id = ?", 1L);
        String token = loginAndGetAccessToken("jerry", "123456");

        mockMvc.perform(post("/user/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文章暂不允许评论"));
    }

    @Test
    void shouldReturnArticleNotFoundWhenCommentArticleMissing() throws Exception {
        String token = loginAndGetAccessToken("jerry", "123456");

        mockMvc.perform(post("/user/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest(999999L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文章不存在"));
    }

    @Test
    void shouldRejectUserReadingOtherAuthorsArticleComments() throws Exception {
        String token = loginAndGetAccessToken("jerry", "123456");

        mockMvc.perform(get("/user/articles/{id}/comments", 2L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldAllowUserReadingOwnArticleComments() throws Exception {
        String token = loginAndGetAccessToken("jerry", "123456");

        mockMvc.perform(get("/user/articles/{id}/comments", 3L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    private Map<String, Object> articleRequest(String title, String slug, String status) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("articleTitle", title);
        request.put("articleSlug", slug);
        request.put("articleSummary", "summary");
        request.put("articleContent", "content");
        request.put("categoryId", 1L);
        request.put("articleStatus", status);
        request.put("tagIds", java.util.List.of(1L, 3L));
        request.put("coverUrl", "https://example.com/ignored.jpg");
        request.put("authorId", 1L);
        return request;
    }

    private Map<String, Object> commentRequest(Long articleId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("articleId", articleId);
        request.put("commentContent", "这是一条新的用户评论");
        return request;
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
