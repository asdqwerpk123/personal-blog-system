package org.example.personalblogsystem.service;

import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class BlogArticleScheduledPublishTest {

    @Autowired
    private IBlogArticleService blogArticleService;

    @Autowired
    private BlogArticleMapper blogArticleMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        Integer columnCount = jdbcTemplate.queryForObject("""
                        select count(*)
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = 'blog_article'
                          and column_name = 'publish_time'
                        """,
                Integer.class);
        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute("alter table blog_article add column publish_time datetime null comment 'scheduled publish time'");
        }
        jdbcTemplate.update("""
                update blog_article
                set deleted = 1,
                    update_time = current_timestamp
                where article_slug like 'scheduled-%'
                """);
    }

    @AfterEach
    void tearDown() {
        AdminAuthContext.clear();
    }

    @Test
    void shouldPublishOnlyDueDraftArticles() {
        LocalDateTime now = LocalDateTime.now();
        Long dueDraftId = insertArticle("DRAFT", now.minusMinutes(1));
        Long futureDraftId = insertArticle("DRAFT", now.plusMinutes(10));
        Long privateDueId = insertArticle("PRIVATE", now.minusMinutes(1));

        int publishedCount = blogArticleService.publishScheduledArticles();

        assertThat(publishedCount).isEqualTo(1);
        BlogArticle dueDraft = blogArticleMapper.selectById(dueDraftId);
        BlogArticle futureDraft = blogArticleMapper.selectById(futureDraftId);
        BlogArticle privateDue = blogArticleMapper.selectById(privateDueId);
        assertThat(dueDraft.getArticleStatus()).isEqualTo("PUBLISHED");
        assertThat(dueDraft.getPublishedTime()).isNotNull();
        assertThat(dueDraft.getUpdateTime()).isAfterOrEqualTo(now.withNano(0));
        assertThat(futureDraft.getArticleStatus()).isEqualTo("DRAFT");
        assertThat(privateDue.getArticleStatus()).isEqualTo("PRIVATE");
    }

    @Test
    void shouldPublishDraftArticleByJobParamArticleId() {
        Long draftId = insertArticle("DRAFT", null);

        int publishedCount = blogArticleService.publishArticleById(draftId);

        assertThat(publishedCount).isEqualTo(1);
        BlogArticle published = blogArticleMapper.selectById(draftId);
        assertThat(published.getArticleStatus()).isEqualTo("PUBLISHED");
        assertThat(published.getPublishedTime()).isNotNull();
        assertThat(published.getUpdateTime()).isNotNull();
    }

    @Test
    void shouldNotPublishNonDraftArticleByJobParamArticleId() {
        Long privateId = insertArticle("PRIVATE", null);

        int publishedCount = blogArticleService.publishArticleById(privateId);

        assertThat(publishedCount).isZero();
        BlogArticle unchanged = blogArticleMapper.selectById(privateId);
        assertThat(unchanged.getArticleStatus()).isEqualTo("PRIVATE");
    }

    @Test
    void shouldPersistScheduledPublishTimeOnCreate() {
        AdminAuthContext.set(new AdminAuthPrincipal(1L, "root", 1L, "SUPER_ADMIN"));
        LocalDateTime publishTime = LocalDateTime.now().plusHours(1).withNano(0);
        BlogArticle article = new BlogArticle();
        article.setArticleTitle("Scheduled create");
        article.setArticleSlug("scheduled-create-" + UUID.randomUUID().toString().substring(0, 8));
        article.setArticleContent("scheduled content");
        article.setCategoryId(1L);
        article.setArticleStatus("DRAFT");
        article.setPublishTime(publishTime);

        BlogArticle created = blogArticleService.createArticle(article);

        BlogArticle persisted = blogArticleMapper.selectById(created.getId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getArticleStatus()).isEqualTo("DRAFT");
        assertThat(persisted.getPublishTime()).isEqualTo(publishTime);
    }

    private Long insertArticle(String status, LocalDateTime publishTime) {
        String slug = "scheduled-" + UUID.randomUUID().toString().substring(0, 12);
        jdbcTemplate.update("""
                        insert into blog_article (
                            article_title, article_slug, article_content, author_id, category_id,
                            article_status, top_flag, allow_comment, view_count, like_count,
                            published_time, publish_time, deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                "Scheduled " + slug,
                slug,
                "content",
                2L,
                1L,
                status,
                false,
                true,
                0,
                0,
                null,
                publishTime,
                false);
        return jdbcTemplate.queryForObject(
                "select id from blog_article where article_slug = ?",
                Long.class,
                slug);
    }
}
