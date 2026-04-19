package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.dto.BlogCategoryCreateRequest;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.example.personalblogsystem.mapper.BlogCategoryMapper;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class BlogCategoryControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoSpyBean
    private BlogCategoryMapper blogCategoryMapper;

    @Autowired
    private BlogArticleMapper blogArticleMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    void tearDown() {
        reset(blogCategoryMapper);
    }

    @Test
    void shouldReturnWrappedCategoryResult() throws Exception {
        mockMvc.perform(get("/admin/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.categoryName").value("Backend"));
    }

    @Test
    void shouldReturnCategoriesOrderedBySortNo() throws Exception {
        mockMvc.perform(get("/admin/category/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].categoryName").value("Backend"))
                .andExpect(jsonPath("$.data[1].categoryName").value("Frontend"))
                .andExpect(jsonPath("$.data[2].categoryName").value("Life"));
    }

    @Test
    void shouldReturnPagedCategories() throws Exception {
        mockMvc.perform(get("/admin/category/page")
                        .param("current", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[0].categoryName").value("Backend"))
                .andExpect(jsonPath("$.data.records[1].categoryName").value("Frontend"));
    }

    @Test
    void shouldFilterPagedCategoriesByKeyword() throws Exception {
        mockMvc.perform(get("/admin/category/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "Vue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(2))
                .andExpect(jsonPath("$.data.records[0].categoryName").value("Frontend"));
    }

    @Test
    void shouldRejectInvalidCategoryPageCurrent() throws Exception {
        mockMvc.perform(get("/admin/category/page")
                        .param("current", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectInvalidCategoryPageSize() throws Exception {
        mockMvc.perform(get("/admin/category/page")
                        .param("current", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectBlankCategoryNameOnCreate() throws Exception {
        BlogCategoryCreateRequest created = createRequest("   ", "temp", null);

        mockMvc.perform(post("/admin/category")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("categoryName must not be blank"));
    }

    @Test
    void shouldRejectUnauthenticatedCreateCategory() throws Exception {
        BlogCategoryCreateRequest created = createRequest("Temp-" + UUID.randomUUID().toString().substring(0, 8), "temp", null);

        mockMvc.perform(post("/admin/category")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldAllowCreateWithoutClientCreatedBy() throws Exception {
        BlogCategoryCreateRequest created = createRequest("Temp-" + UUID.randomUUID().toString().substring(0, 8), "temp", null);

        mockMvc.perform(post("/admin/category")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldRejectDuplicateCategoryNameOnCreate() throws Exception {
        BlogCategoryCreateRequest created = createRequest("Backend", "temp", null);

        mockMvc.perform(post("/admin/category")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("categoryName already exists"));
    }

    @Test
    void shouldTranslateDuplicateKeyFailureOnCreateToBadRequest() throws Exception {
        doThrow(duplicateCategoryNameViolation())
                .when(blogCategoryMapper)
                .insert(any(BlogCategory.class));

        BlogCategoryCreateRequest created = createRequest("Temp-" + UUID.randomUUID().toString().substring(0, 8), "temp", null);

        mockMvc.perform(post("/admin/category")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("categoryName already exists"));
    }

    @Test
    void shouldTranslateCreatedByForeignKeyFailureOnCreateToBadRequest() throws Exception {
        doThrow(invalidCreatedByViolation())
                .when(blogCategoryMapper)
                .insert(any(BlogCategory.class));

        BlogCategoryCreateRequest created = createRequest("Temp-" + UUID.randomUUID().toString().substring(0, 8), "temp", null);

        mockMvc.perform(post("/admin/category")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("createdBy is invalid"));
    }

    @Test
    void shouldNotTranslateDifferentDuplicateViolationOnCreate() throws Exception {
        doThrow(otherDuplicateViolation())
                .when(blogCategoryMapper)
                .insert(any(BlogCategory.class));

        BlogCategoryCreateRequest created = createRequest("Temp-" + UUID.randomUUID().toString().substring(0, 8), "temp", null);

        mockMvc.perform(post("/admin/category")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SYSTEM_ERROR.getCode()));
    }

    @Test
    void shouldNotTranslateDifferentForeignKeyViolationOnCreate() throws Exception {
        doThrow(otherForeignKeyViolation())
                .when(blogCategoryMapper)
                .insert(any(BlogCategory.class));

        BlogCategoryCreateRequest created = createRequest("Temp-" + UUID.randomUUID().toString().substring(0, 8), "temp", null);

        mockMvc.perform(post("/admin/category")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SYSTEM_ERROR.getCode()));
    }

    @Test
    void shouldNotMisTranslateOtherIntegrityViolationOnCreate() throws Exception {
        doThrow(otherIntegrityViolation())
                .when(blogCategoryMapper)
                .insert(any(BlogCategory.class));

        BlogCategoryCreateRequest created = createRequest("Temp-" + UUID.randomUUID().toString().substring(0, 8), "temp", null);

        mockMvc.perform(post("/admin/category")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SYSTEM_ERROR.getCode()));
    }

    @Test
    void shouldIgnoreSpoofedCreatedByAndPersistAuthenticatedOwner() throws Exception {
        LoginSession session = loginAndGetSession("root", "123456");
        String categoryName = "Temp-" + UUID.randomUUID().toString().substring(0, 8);

        String requestJson = """
                {
                  "categoryName": "%s",
                  "description": "temp",
                  "sortNo": 9,
                  "createdBy": 999
                }
                """.formatted(categoryName);

        JsonNode createdNode = performJson(post("/admin/category")
                .header("Authorization", "Bearer " + session.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));

        long categoryId = createdNode.path("data").path("id").asLong();
        BlogCategory persisted = blogCategoryMapper.selectById(categoryId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getCategoryName()).isEqualTo(categoryName);
        assertThat(persisted.getCreatedBy()).isEqualTo(session.userId());
        assertThat(persisted.getSortNo()).isEqualTo(9);
    }

    @Test
    void shouldCreateUpdateAndDeleteCategory() throws Exception {
        BlogCategoryCreateRequest created = createRequest("Temp-" + UUID.randomUUID().toString().substring(0, 8), "temp", 9);

        JsonNode createdNode = performJson(post("/admin/category")
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(created)));

        long categoryId = createdNode.path("data").path("id").asLong();
        assertThat(categoryId).isPositive();

        BlogCategory persisted = blogCategoryMapper.selectById(categoryId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getCategoryName()).isEqualTo(created.getCategoryName());
        assertThat(persisted.getSortNo()).isEqualTo(9);

        BlogCategory updateRequest = new BlogCategory();
        updateRequest.setCategoryName(created.getCategoryName() + "-updated");
        updateRequest.setDescription("updated");
        updateRequest.setSortNo(10);

        performJson(put("/admin/category/{id}", categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        BlogCategory updated = blogCategoryMapper.selectById(categoryId);
        assertThat(updated).isNotNull();
        assertThat(updated.getCategoryName()).isEqualTo(created.getCategoryName() + "-updated");
        assertThat(updated.getDescription()).isEqualTo("updated");
        assertThat(updated.getSortNo()).isEqualTo(10);

        mockMvc.perform(delete("/admin/category/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(blogCategoryMapper.selectById(categoryId)).isNull();
    }

    @Test
    void shouldRejectDuplicateCategoryNameOnUpdate() throws Exception {
        BlogCategory updateRequest = new BlogCategory();
        updateRequest.setCategoryName("Backend");
        updateRequest.setDescription("updated");
        updateRequest.setSortNo(10);

        mockMvc.perform(put("/admin/category/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("categoryName already exists"));
    }

    @Test
    void shouldAllowKeepingSameCategoryNameOnUpdate() throws Exception {
        BlogCategory existing = blogCategoryMapper.selectById(1L);
        assertThat(existing).isNotNull();

        BlogCategory updateRequest = new BlogCategory();
        updateRequest.setCategoryName(existing.getCategoryName());
        updateRequest.setDescription("same name update");
        updateRequest.setSortNo(11);

        mockMvc.perform(put("/admin/category/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.categoryName").value(existing.getCategoryName()));

        BlogCategory updated = blogCategoryMapper.selectById(1L);
        assertThat(updated).isNotNull();
        assertThat(updated.getCategoryName()).isEqualTo(existing.getCategoryName());
        assertThat(updated.getDescription()).isEqualTo("same name update");
        assertThat(updated.getSortNo()).isEqualTo(11);
    }

    @Test
    void shouldTranslateDuplicateKeyFailureOnUpdateToBadRequest() throws Exception {
        doThrow(duplicateCategoryNameViolation())
                .when(blogCategoryMapper)
                .updateById(any(BlogCategory.class));

        BlogCategory updateRequest = new BlogCategory();
        updateRequest.setCategoryName("Backend-updated");
        updateRequest.setDescription("updated");
        updateRequest.setSortNo(10);

        mockMvc.perform(put("/admin/category/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("categoryName already exists"));
    }

    @Test
    void shouldRejectDeletingReferencedCategory() throws Exception {
        BlogCategory category = blogCategoryMapper.selectById(1L);
        assertThat(category).isNotNull();
        BlogArticle article = blogArticleMapper.selectById(1L);
        assertThat(article).isNotNull();
        assertThat(article.getCategoryId()).isEqualTo(category.getId());

        mockMvc.perform(delete("/admin/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("category is referenced by articles"));
    }

    @Test
    void shouldRejectDeletingMissingCategory() throws Exception {
        mockMvc.perform(delete("/admin/category/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingCategory() throws Exception {
        BlogCategory updateRequest = new BlogCategory();
        updateRequest.setCategoryName("Missing");
        updateRequest.setDescription("Missing");
        updateRequest.setSortNo(1);

        mockMvc.perform(put("/admin/category/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldDefaultSortNoToZeroWhenOmittedOnCreate() throws Exception {
        BlogCategoryCreateRequest created = createRequest("Temp-" + UUID.randomUUID().toString().substring(0, 8), "temp", null);

        JsonNode createdNode = performJson(post("/admin/category")
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(created)));

        long categoryId = createdNode.path("data").path("id").asLong();
        BlogCategory persisted = blogCategoryMapper.selectById(categoryId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getSortNo()).isEqualTo(0);
    }

    private JsonNode performJson(org.springframework.test.web.servlet.RequestBuilder requestBuilder) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private DataIntegrityViolationException duplicateCategoryNameViolation() {
        return new DataIntegrityViolationException(
                "translated integrity constraint failure",
                new SQLIntegrityConstraintViolationException(
                        "Duplicate entry for key 'uk_blog_category_category_name'",
                        "23000",
                        1062));
    }

    private DataIntegrityViolationException invalidCreatedByViolation() {
        return new DataIntegrityViolationException(
                "translated integrity constraint failure",
                new SQLIntegrityConstraintViolationException(
                        "Cannot add or update a child row: a foreign key constraint fails (`blog_category`, CONSTRAINT `fk_blog_category_created_by` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`))",
                "23000",
                1452));
    }

    private DataIntegrityViolationException otherDuplicateViolation() {
        return new DataIntegrityViolationException(
                "translated integrity constraint failure",
                new SQLIntegrityConstraintViolationException(
                        "Duplicate entry for key 'uk_blog_category_sort_no'",
                        "23000",
                        1062));
    }

    private DataIntegrityViolationException otherForeignKeyViolation() {
        return new DataIntegrityViolationException(
                "translated integrity constraint failure",
                new SQLIntegrityConstraintViolationException(
                        "Cannot add or update a child row: a foreign key constraint fails (`blog_article`, CONSTRAINT `fk_blog_article_created_by` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`))",
                        "23000",
                        1452));
    }

    private DataIntegrityViolationException otherIntegrityViolation() {
        return new DataIntegrityViolationException(
                "translated integrity constraint failure",
                new SQLIntegrityConstraintViolationException(
                        "Column 'category_name' cannot be null",
                        "23000",
                        1048));
    }

    private String loginAndGetAccessToken(String userName, String password) throws Exception {
        return loginAndGetSession(userName, password).accessToken();
    }

    private LoginSession loginAndGetSession(String userName, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(userName, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return new LoginSession(
                root.path("data").path("accessToken").asText(),
                root.path("data").path("id").asLong());
    }

    private record LoginSession(String accessToken, long userId) {
    }

    private LoginRequest loginRequest(String userName, String password) {
        LoginRequest request = new LoginRequest();
        request.setUserName(userName);
        request.setPassword(password);
        return request;
    }

    private BlogCategoryCreateRequest createRequest(String categoryName, String description, Integer sortNo) {
        BlogCategoryCreateRequest request = new BlogCategoryCreateRequest();
        request.setCategoryName(categoryName);
        request.setDescription(description);
        request.setSortNo(sortNo);
        return request;
    }
}
