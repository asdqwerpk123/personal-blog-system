package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.service.IBlogCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/category")
public class BlogCategoryController {

    private final IBlogCategoryService blogCategoryService;

    public BlogCategoryController(IBlogCategoryService blogCategoryService) {
        this.blogCategoryService = blogCategoryService;
    }

    @GetMapping("/{id}")
    public Result<BlogCategory> getById(@PathVariable Long id) {
        BlogCategory category = blogCategoryService.getById(id);
        return category == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(category);
    }
}