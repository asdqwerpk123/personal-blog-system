package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.entity.BlogTag;
import org.example.personalblogsystem.service.IBlogCategoryService;
import org.example.personalblogsystem.service.IBlogTagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserCategoryController {

    private final IBlogCategoryService blogCategoryService;
    private final IBlogTagService blogTagService;

    public UserCategoryController(IBlogCategoryService blogCategoryService,
                                  IBlogTagService blogTagService) {
        this.blogCategoryService = blogCategoryService;
        this.blogTagService = blogTagService;
    }

    @GetMapping("/categories/list")
    public Result<List<BlogCategory>> categories() {
        return Result.ok(blogCategoryService.lambdaQuery()
                .orderByAsc(BlogCategory::getSortNo, BlogCategory::getId)
                .list());
    }

    @GetMapping("/tags/list")
    public Result<List<BlogTag>> tags() {
        return Result.ok(blogTagService.lambdaQuery()
                .orderByAsc(BlogTag::getId)
                .list());
    }
}
