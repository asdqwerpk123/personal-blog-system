package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.service.IBlogArticleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/article")
public class BlogArticleController {

    private final IBlogArticleService blogArticleService;

    public BlogArticleController(IBlogArticleService blogArticleService) {
        this.blogArticleService = blogArticleService;
    }

    @GetMapping("/{id}")
    public Result<BlogArticle> getById(@PathVariable Long id) {
        BlogArticle article = blogArticleService.getById(id);
        return article == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(article);
    }
}