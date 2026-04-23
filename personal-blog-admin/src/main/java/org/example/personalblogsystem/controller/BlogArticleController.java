package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.service.IBlogArticleService;
import org.example.personalblogsystem.service.IBlogCategoryService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/admin/article")
public class BlogArticleController {

    private static final Set<String> ARTICLE_STATUSES = Set.of("DRAFT", "PUBLISHED", "PRIVATE");

    private final IBlogArticleService blogArticleService;
    private final IBlogCategoryService blogCategoryService;

    public BlogArticleController(IBlogArticleService blogArticleService, IBlogCategoryService blogCategoryService) {
        this.blogArticleService = blogArticleService;
        this.blogCategoryService = blogCategoryService;
    }

    @GetMapping("/page")
    public Result<IPage<BlogArticle>> page(@RequestParam(defaultValue = "1") Long page,
                                           @RequestParam(defaultValue = "10") Long pageSize,
                                           @RequestParam(required = false) String title,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String categoryName,
                                           @RequestParam(required = false) String status) {
        String articleStatus = normalizeStatus(status);
        if (StringUtils.hasText(status) && !ARTICLE_STATUSES.contains(articleStatus)) {
            return Result.fail(ResultCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<BlogArticle> queryWrapper = activeArticleQuery();
        if (StringUtils.hasText(title)) {
            queryWrapper.like(BlogArticle::getArticleTitle, title.trim());
        }
        if (categoryId != null) {
            queryWrapper.eq(BlogArticle::getCategoryId, categoryId);
        } else if (StringUtils.hasText(categoryName)) {
            List<Long> categoryIds = findCategoryIdsByName(categoryName);
            if (categoryIds.isEmpty()) {
                return Result.ok(new Page<>(page, pageSize, 0));
            }
            queryWrapper.in(BlogArticle::getCategoryId, categoryIds);
        }
        if (StringUtils.hasText(articleStatus)) {
            queryWrapper.eq(BlogArticle::getArticleStatus, articleStatus);
        }

        return Result.ok(blogArticleService.page(new Page<>(page, pageSize), queryWrapper));
    }

    @GetMapping("/{id}")
    public Result<BlogArticle> getById(@PathVariable Long id) {
        BlogArticle article = blogArticleService.getById(id);
        return isMissing(article) ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(article);
    }

    @PutMapping("/{id}/status")
    public Result<BlogArticle> updateStatus(@PathVariable Long id, @RequestParam String status) {
        String articleStatus = normalizeStatus(status);
        if (!ARTICLE_STATUSES.contains(articleStatus)) {
            return Result.fail(ResultCodeEnum.PARAM_ERROR);
        }

        BlogArticle article = blogArticleService.getById(id);
        if (isMissing(article)) {
            return Result.fail(ResultCodeEnum.NOT_FOUND);
        }

        article.setArticleStatus(articleStatus);
        if ("PUBLISHED".equals(articleStatus) && article.getPublishedTime() == null) {
            article.setPublishedTime(LocalDateTime.now());
        }

        boolean updated = blogArticleService.updateById(article);
        return updated ? Result.ok(article) : Result.fail(ResultCodeEnum.FAIL);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        BlogArticle article = blogArticleService.getById(id);
        if (isMissing(article)) {
            return Result.fail(ResultCodeEnum.NOT_FOUND);
        }

        article.setDeleted(true);
        boolean deleted = blogArticleService.updateById(article);
        return deleted ? Result.ok(true) : Result.fail(ResultCodeEnum.FAIL);
    }

    private LambdaQueryWrapper<BlogArticle> activeArticleQuery() {
        return new LambdaQueryWrapper<BlogArticle>()
                .eq(BlogArticle::getDeleted, false)
                .orderByDesc(BlogArticle::getPublishedTime)
                .orderByDesc(BlogArticle::getCreateTime);
    }

    private List<Long> findCategoryIdsByName(String categoryName) {
        return blogCategoryService.list(new LambdaQueryWrapper<BlogCategory>()
                        .eq(BlogCategory::getDeleted, false)
                        .like(BlogCategory::getCategoryName, categoryName.trim()))
                .stream()
                .map(BlogCategory::getId)
                .toList();
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "";
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "OFFLINE".equals(normalized) ? "PRIVATE" : normalized;
    }

    private boolean isMissing(BlogArticle article) {
        return article == null || Boolean.TRUE.equals(article.getDeleted());
    }
}
