package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.dto.ArticleTagUpdateRequest;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
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
class BlogArticleControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoSpyBean
    private BlogArticleMapper blogArticleMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = secureMockMvc(webApplicationContext);
    }

    @AfterEach
    void tearDown() {
        reset(blogArticleMapper);
    }

    @Test
    void shouldReturnWrappedArticleResult() throws Exception {
        mockMvc.perform(get("/admin/article/1")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.articleTitle").value("Build a Personal Blog with Spring Boot"));
    }

    @Test
    void shouldReturnPagedArticles() throws Exception {
        Integer expectedTotal = jdbcTemplate.queryForObject(
                "select count(*) from blog_article where deleted = 0",
                Integer.class);

        mockMvc.perform(get("/admin/article/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(expectedTotal));
    }

    @Test
    void shouldFilterPagedArticlesByKeyword() throws Exception {
        mockMvc.perform(get("/admin/article/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
        mockMvc.perform(get("/admin/article/{id}/tags", 1L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].tagName").value("SpringBoot"))
                .andExpect(jsonPath("$.data[1].tagName").value("MySQL"));
    }

    @Test
    void shouldReturnNotFoundWhenGettingTagsForMissingArticle() throws Exception {
        mockMvc.perform(get("/admin/article/{id}/tags", 999L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldRejectUnauthenticatedCreate() throws Exception {
        BlogArticle article = buildArticle("Unauth create", randomSlug(), "draft content", 5L);
        article.setCategoryId(1L);

        mockMvc.perform(post("/admin/article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(article)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectUnauthenticatedUpdate() throws Exception {
        BlogArticle article = buildArticle("Unauth update", randomSlug(), "draft content", 5L);
        article.setCategoryId(1L);

        mockMvc.perform(put("/admin/article/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(article)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectUnauthenticatedStatusUpdate() throws Exception {
        mockMvc.perform(put("/admin/article/{id}/status", 1L)
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectUnauthenticatedTagUpdate() throws Exception {
        ArticleTagUpdateRequest request = new ArticleTagUpdateRequest();
        request.setTagIds(java.util.List.of(1L));

        mockMvc.perform(put("/admin/article/{id}/tags", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectUnauthenticatedDelete() throws Exception {
        mockMvc.perform(delete("/admin/article/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectBlankArticleTitleOnCreate() throws Exception {
        BlogArticle article = buildArticle("   ", randomSlug(), "draft content", 5L);

        mockMvc.perform(post("/admin/article")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(article)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("articleTitle must not be blank"));
    }

    @Test
    void shouldOverrideSpoofedPublishedTimeAndServerManagedCountsOnCreate() throws Exception {
        LocalDateTime spoofedPublishedTime = LocalDateTime.of(2001, 1, 1, 0, 0);
        ObjectNode article = objectMapper.createObjectNode();
        article.put("articleTitle", "Published spoof");
        article.put("articleSlug", randomSlug());
        article.put("articleSummary", "summary");
        article.put("coverUrl", "https://example.com/cover.png");
        article.put("articleContent", "published content");
        article.put("categoryId", 1L);
        article.put("articleStatus", "PUBLISHED");
        article.put("topFlag", true);
        article.put("allowComment", false);
        article.put("publishedTime", spoofedPublishedTime.toString());
        article.put("viewCount", 987);
        article.put("likeCount", 654);

        JsonNode createdNode = performJson(post("/admin/article")
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(article)));

        long articleId = createdNode.path("data").path("id").asLong();
        assertThat(articleId).isPositive();

        BlogArticle persisted = blogArticleMapper.selectById(articleId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getArticleStatus()).isEqualTo("PUBLISHED");
        assertThat(persisted.getPublishedTime()).isNotNull();
        assertThat(persisted.getPublishedTime()).isNotEqualTo(spoofedPublishedTime);
        assertThat(persisted.getViewCount()).isZero();
        assertThat(persisted.getLikeCount()).isZero();
    }

    @Test
    void shouldCreateDraftArticleWithFormattedPublishTime() throws Exception {
        ObjectNode article = objectMapper.createObjectNode();
        article.put("articleTitle", "Scheduled publish article");
        article.put("articleSlug", randomSlug());
        article.put("articleSummary", "summary");
        article.put("coverUrl", "https://example.com/cover.png");
        article.put("articleContent", "draft content");
        article.put("categoryId", 1L);
        article.put("articleStatus", "DRAFT");
        article.put("publishTime", "2026-06-07 18:40:00");
        article.put("topFlag", false);
        article.put("allowComment", true);

        JsonNode createdNode = performJson(post("/admin/article")
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(article)));

        assertThat(createdNode.path("code").asInt()).isEqualTo(200);
        long articleId = createdNode.path("data").path("id").asLong();
        assertThat(articleId).isPositive();

        BlogArticle persisted = blogArticleMapper.selectById(articleId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getArticleStatus()).isEqualTo("DRAFT");
        assertThat(persisted.getPublishTime()).isEqualTo(LocalDateTime.of(2026, 6, 7, 18, 40, 0));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingArticle() throws Exception {
        BlogArticle article = buildArticle("Missing", randomSlug(), "draft content", 5L);

        mockMvc.perform(put("/admin/article/{id}", 999L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(article)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldPreserveExistingPublishedTimeWhenUpdatingArticle() throws Exception {
        BlogArticle created = buildArticle(
                "Temp article " + UUID.randomUUID().toString().substring(0, 8),
                randomSlug(),
                "Initial content",
                5L);
        created.setArticleSummary("Initial summary");
        created.setCategoryId(1L);
        created.setArticleStatus("PUBLISHED");

        long articleId = createArticle(created);
        BlogArticle persistedBeforeUpdate = blogArticleMapper.selectById(articleId);
        assertThat(persistedBeforeUpdate).isNotNull();
        assertThat(persistedBeforeUpdate.getPublishedTime()).isNotNull();

        LocalDateTime originalPublishedTime = persistedBeforeUpdate.getPublishedTime();
        LocalDateTime spoofedPublishedTime = originalPublishedTime.plusYears(1);

        ObjectNode updateRequest = objectMapper.createObjectNode();
        updateRequest.put("articleTitle", created.getArticleTitle() + " updated");
        updateRequest.put("articleSlug", created.getArticleSlug());
        updateRequest.put("articleSummary", "Updated summary");
        updateRequest.put("coverUrl", "https://example.com/updated-cover.png");
        updateRequest.put("articleContent", "Updated content");
        updateRequest.put("categoryId", 2L);
        updateRequest.put("topFlag", true);
        updateRequest.put("allowComment", false);
        updateRequest.put("publishedTime", spoofedPublishedTime.toString());

        JsonNode updateResponse = performJson(put("/admin/article/{id}", articleId)
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        assertThat(updateResponse.path("code").asInt()).isEqualTo(200);

        BlogArticle persistedAfterUpdate = blogArticleMapper.selectById(articleId);
        assertThat(persistedAfterUpdate).isNotNull();
        assertThat(persistedAfterUpdate.getArticleSummary()).isEqualTo("Updated summary");
        assertThat(persistedAfterUpdate.getCoverUrl()).isEqualTo("https://example.com/updated-cover.png");
        assertThat(persistedAfterUpdate.getPublishedTime()).isEqualTo(originalPublishedTime);
        assertThat(persistedAfterUpdate.getPublishedTime()).isNotEqualTo(spoofedPublishedTime);
    }

    @Test
    void shouldUpdateDraftArticleWithFormattedPublishTime() throws Exception {
        BlogArticle created = buildArticle(
                "Temp article " + UUID.randomUUID().toString().substring(0, 8),
                randomSlug(),
                "Initial content",
                5L);
        created.setArticleSummary("Initial summary");
        created.setCategoryId(1L);
        created.setArticleStatus("DRAFT");

        long articleId = createArticle(created);

        ObjectNode updateRequest = objectMapper.createObjectNode();
        updateRequest.put("articleTitle", created.getArticleTitle() + " updated");
        updateRequest.put("articleSlug", created.getArticleSlug());
        updateRequest.put("articleSummary", "Updated summary");
        updateRequest.put("articleContent", "Updated content");
        updateRequest.put("categoryId", 1L);
        updateRequest.put("articleStatus", "DRAFT");
        updateRequest.put("publishTime", "2026-06-07 19:00:00");

        JsonNode updateResponse = performJson(put("/admin/article/{id}", articleId)
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        assertThat(updateResponse.path("code").asInt()).isEqualTo(200);

        BlogArticle persistedAfterUpdate = blogArticleMapper.selectById(articleId);
        assertThat(persistedAfterUpdate).isNotNull();
        assertThat(persistedAfterUpdate.getPublishTime()).isEqualTo(LocalDateTime.of(2026, 6, 7, 19, 0, 0));
    }

    @Test
    void shouldUpdateArticleStatusThroughArticleUpdateRequest() throws Exception {
        BlogArticle created = buildArticle(
                "Temp article " + UUID.randomUUID().toString().substring(0, 8),
                randomSlug(),
                "Initial content",
                5L);
        created.setArticleSummary("Initial summary");
        created.setCategoryId(1L);
        created.setArticleStatus("DRAFT");

        long articleId = createArticle(created);

        ObjectNode updateRequest = objectMapper.createObjectNode();
        updateRequest.put("articleTitle", created.getArticleTitle() + " updated");
        updateRequest.put("articleSlug", created.getArticleSlug());
        updateRequest.put("articleSummary", "Updated summary");
        updateRequest.put("articleContent", "Updated content");
        updateRequest.put("categoryId", 1L);
        updateRequest.put("articleStatus", "PRIVATE");

        mockMvc.perform(put("/admin/article/{id}", articleId)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.articleStatus").value("PRIVATE"));

        BlogArticle persisted = blogArticleMapper.selectById(articleId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getArticleStatus()).isEqualTo("PRIVATE");
    }

    @Test
    void shouldRejectInvalidArticleStatusUpdate() throws Exception {
        mockMvc.perform(put("/admin/article/{id}/status", 1L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("status must be one of DRAFT, PUBLISHED, PRIVATE"));
    }

    @Test
    void shouldAcceptLowercaseArticleStatusInTurkishLocale() throws Exception {
        withLocale(new Locale("tr", "TR"), () -> mockMvc.perform(put("/admin/article/{id}/status", 1L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("status", "private"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.articleStatus").value("PRIVATE")));
    }

    @Test
    void shouldRejectInvalidArticlePageCurrent() throws Exception {
        mockMvc.perform(get("/admin/article/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("current must be greater than 0"));
    }

    @Test
    void shouldRejectInvalidArticlePageSize() throws Exception {
        mockMvc.perform(get("/admin/article/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(article)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("articleSlug already exists"));
    }

    @Test
    void shouldTranslateDuplicateKeyFailureOnCreateToBadRequest() throws Exception {
        doThrow(duplicateConstraintViolation())
                .when(blogArticleMapper).insert(any(BlogArticle.class));

        BlogArticle article = buildArticle("Forced duplicate", randomSlug(), "draft content", 5L);
        article.setArticleSummary("summary");
        article.setCategoryId(1L);

        mockMvc.perform(post("/admin/article")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("articleSlug already exists"));
    }

    @Test
    void shouldTranslateDuplicateKeyFailureOnUpdateToBadRequest() throws Exception {
        BlogArticle updateRequest = buildArticle(
                "Updated title",
                randomSlug(),
                "Updated content",
                5L);
        updateRequest.setArticleSummary("Updated summary");
        updateRequest.setCategoryId(1L);

        doThrow(duplicateConstraintViolation())
                .when(blogArticleMapper).updateById(any(BlogArticle.class));

        mockMvc.perform(put("/admin/article/{id}", 1L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("articleSlug already exists"));
    }

    @Test
    void shouldNotTranslateNonDuplicateConstraintFailureToDuplicateMessage() throws Exception {
        doThrow(nonDuplicateConstraintViolation())
                .when(blogArticleMapper).insert(any(BlogArticle.class));

        BlogArticle article = buildArticle("Foreign key failure", randomSlug(), "draft content", 5L);
        article.setArticleSummary("summary");
        article.setCategoryId(1L);

        mockMvc.perform(post("/admin/article")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(article)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SYSTEM_ERROR.getCode()));
    }

    @Test
    void shouldIgnoreSpoofedAuthorOnCreate() throws Exception {
        BlogArticle article = buildArticle("Spoofed author", randomSlug(), "draft content", 5L);
        article.setCategoryId(1L);

        JsonNode createdNode = performJson(post("/admin/article")
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(article)));

        long articleId = createdNode.path("data").path("id").asLong();
        assertThat(articleId).isPositive();

        BlogArticle persisted = blogArticleMapper.selectById(articleId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getAuthorId()).isEqualTo(1L);
    }

    @Test
    void shouldIgnoreSpoofedAuthorOnUpdate() throws Exception {
        BlogArticle updateRequest = buildArticle(
                "Build a Personal Blog with Spring Boot updated",
                "build-personal-blog-with-spring-boot",
                "Updated content",
                1L);
        updateRequest.setArticleSummary("Updated summary");
        updateRequest.setCategoryId(1L);
        updateRequest.setTopFlag(true);
        updateRequest.setAllowComment(false);

        mockMvc.perform(put("/admin/article/{id}", 1L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        BlogArticle persisted = blogArticleMapper.selectById(1L);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getAuthorId()).isEqualTo(2L);
    }

    @Test
    void shouldRejectInvalidCategoryOnCreate() throws Exception {
        BlogArticle article = buildArticle("Invalid category", randomSlug(), "draft content", 5L);
        article.setCategoryId(999L);

        mockMvc.perform(post("/admin/article")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(removeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArticleTagUpdateRequest restoreRequest = new ArticleTagUpdateRequest();
        restoreRequest.setTagIds(java.util.List.of(1L, 3L));

        mockMvc.perform(put("/admin/article/{id}/tags", 1L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        BlogArticle published = blogArticleMapper.selectById(articleId);
        assertThat(published).isNotNull();
        assertThat(published.getArticleStatus()).isEqualTo("PUBLISHED");
        assertThat(published.getPublishedTime()).isNotNull();

        mockMvc.perform(delete("/admin/article/{id}", articleId)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer deleted = jdbcTemplate.queryForObject(
                "select deleted from blog_article where id = ?",
                Integer.class,
                articleId);
        assertThat(deleted).isEqualTo(1);

        Integer readableDeleteLogs = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where target_type = 'ARTICLE'
                          and target_id = ?
                          and action_type = 'DELETE_ARTICLE'
                          and action_result = 'SUCCESS'
                          and action_detail = ?
                        """,
                Integer.class,
                articleId,
                "删除文章：" + updated.getArticleTitle());
        assertThat(readableDeleteLogs).isEqualTo(1);

        Integer legacyDeleteLogs = jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where target_type = 'ARTICLE'
                          and target_id = ?
                          and action_type = 'LOGIC_DELETE'
                        """,
                Integer.class,
                articleId);
        assertThat(legacyDeleteLogs).isZero();
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
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
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
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldRejectNormalUserDeleteThroughAdminEndpoint() throws Exception {
        mockMvc.perform(delete("/admin/article/{id}", 1L)
                        .header("Authorization", "Bearer " + loginAndGetUserAccessToken("jerry", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
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
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(article)));
        long articleId = createdNode.path("data").path("id").asLong();
        assertThat(articleId).isPositive();
        return articleId;
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

    private String loginAndGetUserAccessToken(String userName, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/user/auth/login")
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

    private JsonNode performJson(org.springframework.test.web.servlet.RequestBuilder requestBuilder) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private DataIntegrityViolationException duplicateConstraintViolation() {
        return new DataIntegrityViolationException(
                "translated integrity constraint failure",
                new SQLIntegrityConstraintViolationException(
                        "Integrity constraint violation",
                        "23000",
                        1062));
    }

    private DataIntegrityViolationException nonDuplicateConstraintViolation() {
        return new DataIntegrityViolationException(
                "translated foreign key failure",
                new SQLIntegrityConstraintViolationException(
                        "Cannot add or update a child row: a foreign key constraint fails",
                        "23000",
                        1452));
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
