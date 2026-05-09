package org.example.personalblogsystem.controller;

import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class PublicControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldPageOnlyPublishedArticlesWithoutAuthorization() throws Exception {
        jdbcTemplate.update("update blog_article set article_status = 'PRIVATE' where id = ?", 2L);

        mockMvc.perform(get("/public/articles/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].articleTitle").value("Build a Personal Blog with Spring Boot"))
                .andExpect(jsonPath("$.data.records[0].viewCount").value(120))
                .andExpect(jsonPath("$.data.records[0].articleContent").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].articleStatus").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].deleted").doesNotExist());
    }

    @Test
    void shouldIgnoreBadAuthorizationOnPublicArticlePage() throws Exception {
        mockMvc.perform(get("/public/articles/page")
                        .header("Authorization", "Bearer bad-token")
                        .param("current", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }

    @Test
    void shouldReturnPublishedArticleDetailAndRejectDraft() throws Exception {
        mockMvc.perform(get("/public/articles/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.articleTitle").value("Build a Personal Blog with Spring Boot"))
                .andExpect(jsonPath("$.data.articleContent").isNotEmpty())
                .andExpect(jsonPath("$.data.viewCount").value(120))
                .andExpect(jsonPath("$.data.articleStatus").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist());

        mockMvc.perform(get("/public/articles/{id}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldReturnOnlyApprovedCommentsAsPagedPublicData() throws Exception {
        jdbcTemplate.update("update blog_comment set comment_status = 'REJECTED' where id = ?", 2L);

        mockMvc.perform(get("/public/articles/{id}/comments", 1L)
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].commentContent").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].commentStatus").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].deleted").doesNotExist());
    }

    @Test
    void shouldReturnOnlyNotDeletedCategoriesAndTags() throws Exception {
        jdbcTemplate.update("update blog_category set deleted = 1 where id = ?", 3L);
        jdbcTemplate.update("update blog_tag set deleted = 1 where id = ?", 4L);

        mockMvc.perform(get("/public/categories/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].categoryName").value("Backend"))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(3))))
                .andExpect(jsonPath("$.data[0].createdBy").doesNotExist())
                .andExpect(jsonPath("$.data[0].deleted").doesNotExist());

        mockMvc.perform(get("/public/tags/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(4))))
                .andExpect(jsonPath("$.data[0].createdBy").doesNotExist())
                .andExpect(jsonPath("$.data[0].deleted").doesNotExist());
    }

    @Test
    void shouldReturnOnlyApprovedFriendLinksWithoutSensitiveFields() throws Exception {
        jdbcTemplate.update("""
                        insert into blog_friend_link
                            (site_name, site_url, site_logo, site_desc, owner_name, contact_email, link_status, created_by, deleted)
                        values (?, ?, ?, ?, ?, ?, 'PENDING', 2, 0)
                        """,
                "Pending Link",
                "https://example.org/" + UUID.randomUUID(),
                "/uploads/friend-links/pending.png",
                "pending",
                "Pending Owner",
                "pending@example.org");

        mockMvc.perform(get("/public/friend-links/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].siteName").value("Open Source Study Notes"))
                .andExpect(jsonPath("$.data[0].linkStatus").doesNotExist())
                .andExpect(jsonPath("$.data[0].contactEmail").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdBy").doesNotExist())
                .andExpect(jsonPath("$.data[0].deleted").doesNotExist());
    }
}
