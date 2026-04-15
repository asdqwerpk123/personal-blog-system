package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.example.personalblogsystem.mapper.BlogCategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class BlogCategoryControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BlogCategoryMapper blogCategoryMapper;

    @Autowired
    private BlogArticleMapper blogArticleMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
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
        BlogCategory created = new BlogCategory();
        created.setCategoryName("   ");
        created.setDescription("temp");
        created.setCreatedBy(1L);

        mockMvc.perform(post("/admin/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("categoryName must not be blank"));
    }

    @Test
    void shouldRejectMissingCreatedByOnCreate() throws Exception {
        BlogCategory created = new BlogCategory();
        created.setCategoryName("Temp-" + UUID.randomUUID().toString().substring(0, 8));
        created.setDescription("temp");

        mockMvc.perform(post("/admin/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("createdBy must not be null"));
    }

    @Test
    void shouldCreateUpdateAndDeleteCategory() throws Exception {
        BlogCategory created = new BlogCategory();
        created.setCategoryName("Temp-" + UUID.randomUUID().toString().substring(0, 8));
        created.setDescription("temp");
        created.setSortNo(9);
        created.setCreatedBy(1L);

        JsonNode createdNode = performJson(post("/admin/category")
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
        BlogCategory created = new BlogCategory();
        created.setCategoryName("Temp-" + UUID.randomUUID().toString().substring(0, 8));
        created.setDescription("temp");
        created.setCreatedBy(1L);

        JsonNode createdNode = performJson(post("/admin/category")
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
}
