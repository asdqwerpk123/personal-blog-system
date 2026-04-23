package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.service.IBlogCategoryService;
import org.example.personalblogsystem.service.IBlogArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlogArticleControllerTest {

    private IBlogArticleService blogArticleService;
    private IBlogCategoryService blogCategoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        blogArticleService = mock(IBlogArticleService.class);
        blogCategoryService = mock(IBlogCategoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BlogArticleController(blogArticleService, blogCategoryService)).build();
    }

    @Test
    void shouldReturnPagedArticles() throws Exception {
        Page<BlogArticle> page = new Page<>(2, 10, 1);
        page.setRecords(List.of(article(101L, "Vue Guide", "PUBLISHED")));
        when(blogArticleService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        mockMvc.perform(get("/admin/article/page")
                        .param("page", "2")
                        .param("pageSize", "10")
                        .param("title", "Vue")
                        .param("categoryId", "3")
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].articleTitle").value("Vue Guide"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldUpdateArticleStatus() throws Exception {
        when(blogArticleService.getById(8L)).thenReturn(article(8L, "Draft", "DRAFT"));
        when(blogArticleService.updateById(any(BlogArticle.class))).thenReturn(true);

        mockMvc.perform(put("/admin/article/8/status").param("status", "PRIVATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.articleStatus").value("PRIVATE"));

        ArgumentCaptor<BlogArticle> articleCaptor = ArgumentCaptor.forClass(BlogArticle.class);
        verify(blogArticleService).updateById(articleCaptor.capture());
        assertEquals(8L, articleCaptor.getValue().getId());
        assertEquals("PRIVATE", articleCaptor.getValue().getArticleStatus());
    }

    @Test
    void shouldLogicallyDeleteArticle() throws Exception {
        when(blogArticleService.getById(8L)).thenReturn(article(8L, "Draft", "DRAFT"));
        when(blogArticleService.updateById(any(BlogArticle.class))).thenReturn(true);

        mockMvc.perform(delete("/admin/article/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        ArgumentCaptor<BlogArticle> articleCaptor = ArgumentCaptor.forClass(BlogArticle.class);
        verify(blogArticleService).updateById(articleCaptor.capture());
        assertEquals(8L, articleCaptor.getValue().getId());
        assertTrue(articleCaptor.getValue().getDeleted());
    }

    private BlogArticle article(Long id, String title, String status) {
        BlogArticle article = new BlogArticle();
        article.setId(id);
        article.setArticleTitle(title);
        article.setCategoryId(3L);
        article.setArticleStatus(status);
        article.setViewCount(12);
        article.setPublishedTime(LocalDateTime.of(2024, 4, 10, 14, 30));
        article.setDeleted(false);
        return article;
    }
}
