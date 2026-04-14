package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.dto.ArticleTagUpdateRequest;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class BlogArticleControllerTest {

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
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldReturnWrappedArticleResult() throws Exception {
        mockMvc.perform(get("/admin/article/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.articleTitle").value("Build a Personal Blog with Spring Boot"));
    }

    @Test
    void shouldReturnPagedArticles() throws Exception {
        mockMvc.perform(get("/admin/article/page")
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
    void shouldFilterPagedArticlesByKeyword() throws Exception {
        mockMvc.perform(get("/admin/article/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "Vue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(2))
                .andExpect(jsonPath("$.data.records[0].articleTitle").value("Vue Frontend Notes for Blog Project"));
    }

    @Test
    void shouldReturnTagsForArticle() throws Exception {
        mockMvc.perform(get("/admin/article/{id}/tags", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].tagName").value("SpringBoot"))
                .andExpect(jsonPath("$.data[1].tagName").value("MySQL"));
    }

    @Test
    void shouldReturnNotFoundWhenGettingTagsForMissingArticle() throws Exception {
        mockMvc.perform(get("/admin/article/{id}/tags", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldRejectBlankArticleTitleOnCreate() throws Exception {
        BlogArticle article = buildArticle("   ", randomSlug(), "draft content", 5L);

        mockMvc.perform(post("/admin/article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(article)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("articleTitle must not be blank"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingArticle() throws Exception {
        BlogArticle article = buildArticle("Missing", randomSlug(), "draft content", 5L);

        mockMvc.perform(put("/admin/article/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(article)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldRejectInvalidArticleStatusUpdate() throws Exception {
        mockMvc.perform(put("/admin/article/{id}/status", 1L)
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("status must be one of DRAFT, PUBLISHED, PRIVATE"));
    }

    @Test
    void shouldRejectInvalidArticlePageCurrent() throws Exception {
        mockMvc.perform(get("/admin/article/page")
                        .param("current", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("current must be greater than 0"));
    }

    @Test
    void shouldRejectInvalidArticlePageSize() throws Exception {
        mockMvc.perform(get("/admin/article/page")
                        .param("current", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("size must not exceed 100"));
    }

    @Test
    void shouldRejectDuplicateSlugOnCreate() throws Exception {
        BlogArticle article = buildArticle("Duplicate slug", "build-personal-blog-with-spring-boot", "draft content", 5L);

        mockMvc.perform(post("/admin/article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(article)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("articleSlug already exists"));
    }

    @Test
    void shouldRejectDuplicateSlugOnUpdate() throws Exception {
        BlogArticle article = buildArticle(
                "Temp article " + UUID.randomUUID().toString().substring(0, 8),
                randomSlug(),
                "Initial content",
                5L);
        article.setArticleSummary("Initial summary");
        article.setCategoryId(1L);

        long articleId = createArticle(article);

        BlogArticle updateRequest = buildArticle(
                "Temp article updated",
                "build-personal-blog-with-spring-boot",
                "Updated content",
                5L);
        updateRequest.setArticleSummary("Updated summary");
        updateRequest.setCategoryId(1L);
        updateRequest.setTopFlag(true);
        updateRequest.setAllowComment(true);

        mockMvc.perform(put("/admin/article/{id}", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("articleSlug already exists"));
    }

    @Test
    void shouldRejectInvalidAuthorOnCreate() throws Exception {
        BlogArticle article = buildArticle("Invalid author", randomSlug(), "draft content", 999L);
        article.setCategoryId(1L);

        mockMvc.perform(post("/admin/article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(article)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("authorId does not exist"));
    }

    @Test
    void shouldRejectInvalidCategoryOnCreate() throws Exception {
        BlogArticle article = buildArticle("Invalid category", randomSlug(), "draft content", 5L);
        article.setCategoryId(999L);

        mockMvc.perform(post("/admin/article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(article)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("categoryId does not exist"));
    }

    @Test
    void shouldReplaceArticleTags() throws Exception {
        ArticleTagUpdateRequest request = new ArticleTagUpdateRequest();
        request.setTagIds(java.util.List.of(2L, 4L, 4L));

        mockMvc.perform(put("/admin/article/{id}/tags", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].tagName").value("Vue"))
                .andExpect(jsonPath("$.data[1].tagName").value("Study"));
    }

    @Test
    void shouldClearArticleTagsWhenRequestContainsEmptyList() throws Exception {
        ArticleTagUpdateRequest request = new ArticleTagUpdateRequest();
        request.setTagIds(java.util.List.of());

        mockMvc.perform(put("/admin/article/{id}/tags", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));

        Integer activeRelationCount = jdbcTemplate.queryForObject(
                "select count(*) from blog_article_tag where article_id = ? and deleted = 0",
                Integer.class,
                3L);
        assertThat(activeRelationCount).isZero();
    }

    @Test
    void shouldRestoreSoftDeletedArticleTagRelation() throws Exception {
        ArticleTagUpdateRequest removeRequest = new ArticleTagUpdateRequest();
        removeRequest.setTagIds(java.util.List.of(1L));

        mockMvc.perform(put("/admin/article/{id}/tags", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(removeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArticleTagUpdateRequest restoreRequest = new ArticleTagUpdateRequest();
        restoreRequest.setTagIds(java.util.List.of(1L, 3L));

        mockMvc.perform(put("/admin/article/{id}/tags", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));

        Integer relationCount = jdbcTemplate.queryForObject(
                "select count(*) from blog_article_tag where article_id = ? and tag_id = ?",
                Integer.class,
                1L,
                3L);
        Integer deletedFlag = jdbcTemplate.queryForObject(
                "select deleted from blog_article_tag where article_id = ? and tag_id = ?",
                Integer.class,
                1L,
                3L);
        assertThat(relationCount).isEqualTo(1);
        assertThat(deletedFlag).isZero();
    }

    @Test
    void shouldRejectReplacingTagsWhenArticleDoesNotExist() throws Exception {
        ArticleTagUpdateRequest request = new ArticleTagUpdateRequest();
        request.setTagIds(java.util.List.of(1L));

        mockMvc.perform(put("/admin/article/{id}/tags", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("articleId does not exist"));
    }

    @Test
    void shouldRejectReplacingTagsWhenTagDoesNotExist() throws Exception {
        ArticleTagUpdateRequest request = new ArticleTagUpdateRequest();
        request.setTagIds(java.util.List.of(999L));

        mockMvc.perform(put("/admin/article/{id}/tags", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("tagId does not exist: 999"));
    }

    @Test
    void shouldCreateUpdateStatusAndDeleteArticle() throws Exception {
        BlogArticle created = buildArticle(
                "Temp article " + UUID.randomUUID().toString().substring(0, 8),
                randomSlug(),
                "Initial content",
                5L);
        created.setArticleSummary("Initial summary");
        created.setCategoryId(1L);

        JsonNode createdNode = performJson(post("/admin/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(created)));

        long articleId = createdNode.path("data").path("id").asLong();
        assertThat(articleId).isPositive();

        BlogArticle persisted = blogArticleMapper.selectById(articleId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getArticleTitle()).isEqualTo(created.getArticleTitle());
        assertThat(persisted.getArticleStatus()).isEqualTo("DRAFT");
        assertThat(persisted.getTopFlag()).isFalse();
        assertThat(persisted.getAllowComment()).isTrue();
        assertThat(persisted.getViewCount()).isZero();
        assertThat(persisted.getLikeCount()).isZero();

        BlogArticle updateRequest = buildArticle(
                created.getArticleTitle() + " updated",
                created.getArticleSlug(),
                "Updated content",
                5L);
        updateRequest.setArticleSummary("Updated summary");
        updateRequest.setCategoryId(2L);
        updateRequest.setTopFlag(true);
        updateRequest.setAllowComment(false);

        performJson(put("/admin/article/{id}", articleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        BlogArticle updated = blogArticleMapper.selectById(articleId);
        assertThat(updated).isNotNull();
        assertThat(updated.getArticleTitle()).isEqualTo(created.getArticleTitle() + " updated");
        assertThat(updated.getArticleSummary()).isEqualTo("Updated summary");
        assertThat(updated.getArticleContent()).isEqualTo("Updated content");
        assertThat(updated.getCategoryId()).isEqualTo(2L);
        assertThat(updated.getTopFlag()).isTrue();
        assertThat(updated.getAllowComment()).isFalse();

        mockMvc.perform(put("/admin/article/{id}/status", articleId)
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        BlogArticle published = blogArticleMapper.selectById(articleId);
        assertThat(published).isNotNull();
        assertThat(published.getArticleStatus()).isEqualTo("PUBLISHED");
        assertThat(published.getPublishedTime()).isNotNull();

        mockMvc.perform(delete("/admin/article/{id}", articleId)
                        .param("operatorUserId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer deleted = jdbcTemplate.queryForObject(
                "select deleted from blog_article where id = ?",
                Integer.class,
                articleId);
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    void shouldPreserveFlagsWhenUpdateRequestOmitsThem() throws Exception {
        BlogArticle created = buildArticle(
                "Temp article " + UUID.randomUUID().toString().substring(0, 8),
                randomSlug(),
                "Initial content",
                5L);
        created.setArticleSummary("Initial summary");
        created.setCategoryId(1L);
        created.setTopFlag(true);
        created.setAllowComment(false);

        long articleId = createArticle(created);

        BlogArticle updateRequest = buildArticle(
                created.getArticleTitle() + " updated",
                created.getArticleSlug(),
                "Updated content",
                5L);
        updateRequest.setArticleSummary("Updated summary");
        updateRequest.setCategoryId(2L);
        updateRequest.setTopFlag(null);
        updateRequest.setAllowComment(null);

        performJson(put("/admin/article/{id}", articleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        BlogArticle updated = blogArticleMapper.selectById(articleId);
        assertThat(updated).isNotNull();
        assertThat(updated.getTopFlag()).isTrue();
        assertThat(updated.getAllowComment()).isFalse();
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingArticle() throws Exception {
        mockMvc.perform(delete("/admin/article/{id}", 999L)
                        .param("operatorUserId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldRejectDeleteWhenOperatorLacksPermission() throws Exception {
        mockMvc.perform(delete("/admin/article/{id}", 1L)
                        .param("operatorUserId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Normal user can only delete own article."));
    }

    private BlogArticle buildArticle(String title, String slug, String content, Long authorId) {
        BlogArticle article = new BlogArticle();
        article.setArticleTitle(title);
        article.setArticleSlug(slug);
        article.setArticleContent(content);
        article.setAuthorId(authorId);
        return article;
    }

    private String randomSlug() {
        return "article-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private long createArticle(BlogArticle article) throws Exception {
        JsonNode createdNode = performJson(post("/admin/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(article)));
        long articleId = createdNode.path("data").path("id").asLong();
        assertThat(articleId).isPositive();
        return articleId;
    }

    private JsonNode performJson(org.springframework.test.web.servlet.RequestBuilder requestBuilder) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
