package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.dto.ArticleTagUpdateRequest;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogTag;
import org.example.personalblogsystem.service.IBlogArticleTagService;
import org.example.personalblogsystem.service.IBlogArticleService;
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
import java.util.Locale;

@RestController
@RequestMapping("/admin/article")
public class BlogArticleController {

    private static final long MAX_PAGE_SIZE = 100L;

    private final IBlogArticleService blogArticleService;
    private final IBlogArticleTagService blogArticleTagService;

    public BlogArticleController(IBlogArticleService blogArticleService,
                                 IBlogArticleTagService blogArticleTagService) {
        this.blogArticleService = blogArticleService;
        this.blogArticleTagService = blogArticleTagService;
    }

    @GetMapping("/{id}")
    public Result<BlogArticle> getById(@PathVariable Long id) {
        BlogArticle article = blogArticleService.getById(id);
        return article == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(article);
    }

    @GetMapping("/page")
    public Result<Page<BlogArticle>> page(@RequestParam long current,
                                          @RequestParam long size,
                                          @RequestParam(required = false) String keyword) {
        validatePageRequest(current, size);
        return Result.ok(blogArticleService.pageArticles(current, size, keyword));
    }

    @GetMapping("/{id}/tags")
    public Result<List<BlogTag>> getTags(@PathVariable Long id) {
        BlogArticle article = blogArticleService.getById(id);
        return article == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(blogArticleTagService.listTagsByArticleId(id));
    }

    @PostMapping
    public Result<BlogArticle> create(@RequestBody BlogArticle article) {
        validateArticleForCreate(article);
        return Result.ok(blogArticleService.createArticle(article));
    }

    @PutMapping("/{id}/tags")
    public Result<List<BlogTag>> updateTags(@PathVariable Long id, @RequestBody ArticleTagUpdateRequest request) {
        validateArticleTagUpdateRequest(request);
        return Result.ok(blogArticleTagService.replaceArticleTags(id, request.getTagIds()));
    }

    @PutMapping("/{id}")
    public Result<BlogArticle> update(@PathVariable Long id, @RequestBody BlogArticle article) {
        validateArticleForUpdate(article);
        BlogArticle updatedArticle = blogArticleService.updateArticle(id, article);
        return updatedArticle == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(updatedArticle);
    }

    @PutMapping("/{id}/status")
    public Result<BlogArticle> updateStatus(@PathVariable Long id, @RequestParam String status) {
        validateStatus(status);
        BlogArticle updatedArticle = blogArticleService.updateArticleStatus(id, status);
        return updatedArticle == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(updatedArticle);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long operatorUserId) {
        return blogArticleService.deleteArticle(id, operatorUserId) ? Result.ok(null) : Result.fail(ResultCodeEnum.NOT_FOUND);
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

    private void validateArticleForCreate(BlogArticle article) {
        validateArticle(article);
        if (article.getAuthorId() == null) {
            throw new IllegalArgumentException("authorId must not be null");
        }
    }

    private void validateArticleForUpdate(BlogArticle article) {
        validateArticle(article);
        if (article.getAuthorId() == null) {
            throw new IllegalArgumentException("authorId must not be null");
        }
    }

    private void validateArticle(BlogArticle article) {
        if (article == null || !StringUtils.hasText(article.getArticleTitle())) {
            throw new IllegalArgumentException("articleTitle must not be blank");
        }
        if (!StringUtils.hasText(article.getArticleSlug())) {
            throw new IllegalArgumentException("articleSlug must not be blank");
        }
        if (!StringUtils.hasText(article.getArticleContent())) {
            throw new IllegalArgumentException("articleContent must not be blank");
        }
    }

    private void validateStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("status must be one of DRAFT, PUBLISHED, PRIVATE");
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"DRAFT".equals(normalized) && !"PUBLISHED".equals(normalized) && !"PRIVATE".equals(normalized)) {
            throw new IllegalArgumentException("status must be one of DRAFT, PUBLISHED, PRIVATE");
        }
    }

    private void validateArticleTagUpdateRequest(ArticleTagUpdateRequest request) {
        if (request == null || request.getTagIds() == null) {
            throw new IllegalArgumentException("tagIds must not be null");
        }
    }
}
