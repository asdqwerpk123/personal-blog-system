package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.entity.BlogTag;
import org.example.personalblogsystem.mapper.BlogTagMapper;
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
class BlogTagControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BlogTagMapper blogTagMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldReturnPagedTags() throws Exception {
        mockMvc.perform(get("/admin/tag/page")
                        .param("current", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(4));
    }

    @Test
    void shouldFilterPagedTagsByKeyword() throws Exception {
        mockMvc.perform(get("/admin/tag/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "Vue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].tagName").value("Vue"));
    }

    @Test
    void shouldRejectInvalidTagPageCurrent() throws Exception {
        mockMvc.perform(get("/admin/tag/page")
                        .param("current", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("current must be greater than 0"));
    }

    @Test
    void shouldRejectInvalidTagPageSize() throws Exception {
        mockMvc.perform(get("/admin/tag/page")
                        .param("current", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("size must not exceed 100"));
    }

    @Test
    void shouldRejectBlankTagNameOnCreate() throws Exception {
        BlogTag tag = new BlogTag();
        tag.setTagName("   ");
        tag.setDescription("temp");
        tag.setCreatedBy(2L);

        mockMvc.perform(post("/admin/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("tagName must not be blank"));
    }

    @Test
    void shouldRejectMissingCreatedByOnCreate() throws Exception {
        BlogTag tag = new BlogTag();
        tag.setTagName("Temp-" + UUID.randomUUID().toString().substring(0, 8));
        tag.setDescription("temp");

        mockMvc.perform(post("/admin/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("createdBy must not be null"));
    }

    @Test
    void shouldRejectInvalidCreatedByOnCreate() throws Exception {
        BlogTag tag = new BlogTag();
        tag.setTagName("Temp-" + UUID.randomUUID().toString().substring(0, 8));
        tag.setDescription("temp");
        tag.setCreatedBy(999L);

        mockMvc.perform(post("/admin/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("createdBy does not exist"));
    }

    @Test
    void shouldRejectDuplicateTagNameOnCreate() throws Exception {
        BlogTag tag = new BlogTag();
        tag.setTagName("SpringBoot");
        tag.setDescription("duplicate");
        tag.setCreatedBy(2L);

        mockMvc.perform(post("/admin/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("tagName already exists"));
    }

    @Test
    void shouldCreateUpdateAndDeleteTag() throws Exception {
        BlogTag created = new BlogTag();
        created.setTagName("Temp-" + UUID.randomUUID().toString().substring(0, 8));
        created.setDescription("temp");
        created.setCreatedBy(2L);

        JsonNode createdNode = performJson(post("/admin/tag")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(created)));

        long tagId = createdNode.path("data").path("id").asLong();
        assertThat(tagId).isPositive();

        BlogTag persisted = blogTagMapper.selectById(tagId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getTagName()).isEqualTo(created.getTagName());

        BlogTag updateRequest = new BlogTag();
        updateRequest.setTagName(created.getTagName() + "-updated");
        updateRequest.setDescription("updated");

        performJson(put("/admin/tag/{id}", tagId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        BlogTag updated = blogTagMapper.selectById(tagId);
        assertThat(updated).isNotNull();
        assertThat(updated.getTagName()).isEqualTo(created.getTagName() + "-updated");
        assertThat(updated.getDescription()).isEqualTo("updated");

        mockMvc.perform(delete("/admin/tag/{id}", tagId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(blogTagMapper.selectById(tagId)).isNull();
    }

    @Test
    void shouldRejectDeletingReferencedTag() throws Exception {
        mockMvc.perform(delete("/admin/tag/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("tag is referenced by articles"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingTag() throws Exception {
        BlogTag updateRequest = new BlogTag();
        updateRequest.setTagName("Missing");
        updateRequest.setDescription("Missing");

        mockMvc.perform(put("/admin/tag/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingTag() throws Exception {
        mockMvc.perform(delete("/admin/tag/{id}", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    private JsonNode performJson(org.springframework.test.web.servlet.RequestBuilder requestBuilder) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
