package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.service.IBlogCategoryService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/category")
public class BlogCategoryController {

    private static final long MAX_PAGE_SIZE = 100L;

    private final IBlogCategoryService blogCategoryService;

    public BlogCategoryController(IBlogCategoryService blogCategoryService) {
        this.blogCategoryService = blogCategoryService;
    }

    @GetMapping("/{id}")
    public Result<BlogCategory> getById(@PathVariable Long id) {
        BlogCategory category = blogCategoryService.getById(id);
        return category == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(category);
    }

    @GetMapping("/list")
    public Result<List<BlogCategory>> list() {
        return Result.ok(blogCategoryService.listCategories());
    }

    @GetMapping("/page")
    public Result<Page<BlogCategory>> page(@RequestParam long current,
                                           @RequestParam long size,
                                           @RequestParam(required = false) String keyword) {
        validatePageRequest(current, size);
        return Result.ok(blogCategoryService.pageCategories(current, size, keyword));
    }

    @PostMapping
    public Result<BlogCategory> create(@RequestBody BlogCategory category) {
        validateCategoryForCreate(category);
        return Result.ok(blogCategoryService.createCategory(category));
    }

    @PutMapping("/{id}")
    public Result<BlogCategory> update(@PathVariable Long id, @RequestBody BlogCategory category) {
        validateCategoryForUpdate(category);
        BlogCategory updatedCategory = blogCategoryService.updateCategory(id, category);
        return updatedCategory == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return blogCategoryService.deleteCategory(id) ? Result.ok(null) : Result.fail(ResultCodeEnum.NOT_FOUND);
    }

    private void validatePageRequest(long current, long size) {
        if (current <= 0) {
            throw new IllegalArgumentException("current must be greater than 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must not exceed " + MAX_PAGE_SIZE);
        }
    }

    private void validateCategoryForCreate(BlogCategory category) {
        validateCategoryName(category);
        if (category.getCreatedBy() == null) {
            throw new IllegalArgumentException("createdBy must not be null");
        }
    }

    private void validateCategoryForUpdate(BlogCategory category) {
        validateCategoryName(category);
    }

    private void validateCategoryName(BlogCategory category) {
        if (category == null || !StringUtils.hasText(category.getCategoryName())) {
            throw new IllegalArgumentException("categoryName must not be blank");
        }
    }
}
