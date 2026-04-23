package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.dto.BlogCategoryCreateRequest;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.service.IBlogCategoryService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/category")
public class BlogCategoryController {

    private static final long DEFAULT_CURRENT = 1L;
    private static final long DEFAULT_SIZE = 10L;
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
    public Result<Page<BlogCategory>> page(@RequestParam(required = false) Long current,
                                           @RequestParam(required = false) Long size,
                                           @RequestParam(required = false) Long page,
                                           @RequestParam(required = false) Long pageSize,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String categoryName) {
        long resolvedCurrent = current != null ? current : page == null ? DEFAULT_CURRENT : page;
        long resolvedSize = size != null ? size : pageSize == null ? DEFAULT_SIZE : pageSize;
        validatePageRequest(resolvedCurrent, resolvedSize);
        String searchKeyword = StringUtils.hasText(keyword) ? keyword : categoryName;
        return Result.ok(blogCategoryService.pageCategories(resolvedCurrent, resolvedSize, searchKeyword));
    }

    @PostMapping
    public Result<BlogCategory> create(@RequestBody BlogCategoryCreateRequest request) {
        validateCategoryForCreate(request);
        return Result.ok(blogCategoryService.createCategory(toCategory(request)));
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

    private void validateCategoryForCreate(BlogCategoryCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getCategoryName())) {
            throw new IllegalArgumentException("categoryName must not be blank");
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

    private BlogCategory toCategory(BlogCategoryCreateRequest request) {
        BlogCategory category = new BlogCategory();
        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setSortNo(request.getSortNo());
        return category;
    }
}
