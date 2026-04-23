package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.service.IBlogCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlogCategoryControllerTest {

    private IBlogCategoryService blogCategoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        blogCategoryService = mock(IBlogCategoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BlogCategoryController(blogCategoryService)).build();
    }

    @Test
    void shouldReturnPagedCategories() throws Exception {
        Page<BlogCategory> page = new Page<>(2, 10, 1);
        page.setRecords(List.of(category(1L, "前端开发", 1)));
        when(blogCategoryService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        mockMvc.perform(get("/admin/category/page")
                        .param("page", "2")
                        .param("pageSize", "10")
                        .param("categoryName", "前端"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].categoryName").value("前端开发"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldReturnCategoryList() throws Exception {
        when(blogCategoryService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(category(1L, "前端开发", 1), category(2L, "后端开发", 2)));

        mockMvc.perform(get("/admin/category/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[1].categoryName").value("后端开发"));
    }

    @Test
    void shouldCreateCategory() throws Exception {
        when(blogCategoryService.save(any(BlogCategory.class))).thenReturn(true);

        BlogCategory request = category(null, "数据库", 6);
        request.setDescription("MySQL");

        mockMvc.perform(post("/admin/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.categoryName").value("数据库"));

        ArgumentCaptor<BlogCategory> categoryCaptor = ArgumentCaptor.forClass(BlogCategory.class);
        verify(blogCategoryService).save(categoryCaptor.capture());
        assertEquals(1L, categoryCaptor.getValue().getCreatedBy());
        assertFalse(categoryCaptor.getValue().getDeleted());
    }

    @Test
    void shouldUpdateCategory() throws Exception {
        when(blogCategoryService.getById(2L)).thenReturn(category(2L, "后端开发", 2));
        when(blogCategoryService.updateById(any(BlogCategory.class))).thenReturn(true);

        BlogCategory request = category(null, "后端开发", 3);
        request.setDescription("Java 和 Spring Boot");

        mockMvc.perform(put("/admin/category/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.sortNo").value(3));

        ArgumentCaptor<BlogCategory> categoryCaptor = ArgumentCaptor.forClass(BlogCategory.class);
        verify(blogCategoryService).updateById(categoryCaptor.capture());
        assertEquals(2L, categoryCaptor.getValue().getId());
        assertEquals("Java 和 Spring Boot", categoryCaptor.getValue().getDescription());
    }

    @Test
    void shouldLogicallyDeleteCategory() throws Exception {
        when(blogCategoryService.getById(2L)).thenReturn(category(2L, "后端开发", 2));
        when(blogCategoryService.updateById(any(BlogCategory.class))).thenReturn(true);

        mockMvc.perform(delete("/admin/category/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        ArgumentCaptor<BlogCategory> categoryCaptor = ArgumentCaptor.forClass(BlogCategory.class);
        verify(blogCategoryService).updateById(categoryCaptor.capture());
        assertEquals(2L, categoryCaptor.getValue().getId());
        assertTrue(categoryCaptor.getValue().getDeleted());
    }

    private BlogCategory category(Long id, String categoryName, Integer sortNo) {
        BlogCategory category = new BlogCategory();
        category.setId(id);
        category.setCategoryName(categoryName);
        category.setDescription(categoryName + "描述");
        category.setSortNo(sortNo);
        category.setCreateTime(LocalDateTime.of(2024, 1, 15, 10, 23));
        category.setDeleted(false);
        return category;
    }

    private String toJson(BlogCategory category) {
        return """
                {
                  "categoryName": "%s",
                  "description": "%s",
                  "sortNo": %d
                }
                """.formatted(category.getCategoryName(), category.getDescription(), category.getSortNo());
    }
}
