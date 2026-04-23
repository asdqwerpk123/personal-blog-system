package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/category")
public class BlogCategoryController {

    private final IBlogCategoryService blogCategoryService;

    public BlogCategoryController(IBlogCategoryService blogCategoryService) {
        this.blogCategoryService = blogCategoryService;
    }

    @GetMapping("/page")
    public Result<IPage<BlogCategory>> page(@RequestParam(defaultValue = "1") Long page,
                                            @RequestParam(defaultValue = "10") Long pageSize,
                                            @RequestParam(required = false) String categoryName) {
        LambdaQueryWrapper<BlogCategory> queryWrapper = activeCategoryQuery();
        if (StringUtils.hasText(categoryName)) {
            queryWrapper.like(BlogCategory::getCategoryName, categoryName.trim());
        }

        IPage<BlogCategory> categoryPage = blogCategoryService.page(new Page<>(page, pageSize), queryWrapper);
        return Result.ok(categoryPage);
    }

    @GetMapping("/list")
    public Result<List<BlogCategory>> list() {
        return Result.ok(blogCategoryService.list(activeCategoryQuery()));
    }

    @GetMapping("/{id}")
    public Result<BlogCategory> getById(@PathVariable Long id) {
        BlogCategory category = blogCategoryService.getById(id);
        return isMissing(category) ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(category);
    }

    @PostMapping
    public Result<BlogCategory> create(@RequestBody BlogCategory request) {
        request.setId(null);
        request.setDeleted(false);
        if (request.getCreatedBy() == null) {
            request.setCreatedBy(1L);
        }

        boolean saved = blogCategoryService.save(request);
        return saved ? Result.ok(request) : Result.fail(ResultCodeEnum.FAIL);
    }

    @PutMapping("/{id}")
    public Result<BlogCategory> update(@PathVariable Long id, @RequestBody BlogCategory request) {
        BlogCategory category = blogCategoryService.getById(id);
        if (isMissing(category)) {
            return Result.fail(ResultCodeEnum.NOT_FOUND);
        }

        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setSortNo(request.getSortNo());

        boolean updated = blogCategoryService.updateById(category);
        return updated ? Result.ok(category) : Result.fail(ResultCodeEnum.FAIL);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        BlogCategory category = blogCategoryService.getById(id);
        if (isMissing(category)) {
            return Result.fail(ResultCodeEnum.NOT_FOUND);
        }

        category.setDeleted(true);
        boolean deleted = blogCategoryService.updateById(category);
        return deleted ? Result.ok(true) : Result.fail(ResultCodeEnum.FAIL);
    }

    private LambdaQueryWrapper<BlogCategory> activeCategoryQuery() {
        return new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getDeleted, false)
                .orderByAsc(BlogCategory::getSortNo)
                .orderByDesc(BlogCategory::getCreateTime);
    }

    private boolean isMissing(BlogCategory category) {
        return category == null || Boolean.TRUE.equals(category.getDeleted());
    }
}
