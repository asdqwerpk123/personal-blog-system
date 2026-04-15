package org.example.personalblogsystem.service;

import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.mapper.BlogArticleTagMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
class BlogArticleTagServiceTransactionTest {

    @Autowired
    private IBlogArticleTagService blogArticleTagService;

    @MockitoSpyBean
    private BlogArticleTagMapper blogArticleTagMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRollbackArticleTagReplacementWhenDeleteFails() {
        normalizeArticleOneTags();

        doThrow(new DuplicateKeyException("forced failure"))
                .when(blogArticleTagMapper)
                .logicDeleteById(anyLong());

        assertThatThrownBy(() -> blogArticleTagService.replaceArticleTags(1L, List.of(2L, 4L)))
                .isInstanceOf(DuplicateKeyException.class);

        List<Long> activeTagIds = jdbcTemplate.queryForList(
                "select tag_id from blog_article_tag where article_id = ? and deleted = 0 order by tag_id",
                Long.class,
                1L);

        assertThat(activeTagIds).containsExactly(1L, 3L);
    }

    private void normalizeArticleOneTags() {
        jdbcTemplate.update(
                "delete from blog_article_tag where article_id = ? and tag_id in (?, ?)",
                1L,
                2L,
                4L);
        jdbcTemplate.update(
                "update blog_article_tag set deleted = 0 where article_id = ? and tag_id in (?, ?)",
                1L,
                1L,
                3L);
    }
}
