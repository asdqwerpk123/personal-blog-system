package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.dto.ArticleTagUpdateRequest;
import org.example.personalblogsystem.dto.BlogArticleCreateRequest;
import org.example.personalblogsystem.dto.BlogArticleUpdateRequest;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.entity.BlogTag;
import org.example.personalblogsystem.service.IBlogArticleService;
import org.example.personalblogsystem.service.IBlogArticleTagService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/admin/article")
public class BlogArticleController {

    private static final long DEFAULT_CURRENT = 1L;
    private static final long DEFAULT_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;
    private static final Set<String> ARTICLE_STATUSES = Set.of("DRAFT", "PUBLISHED", "PRIVATE");

    private final IBlogArticleService blogArticleService;
    private final IBlogArticleTagService blogArticleTagService;
    private final IBlogCategoryService blogCategoryService;

    public BlogArticleController(IBlogArticleService blogArticleService,
                                 IBlogArticleTagService blogArticleTagService,
                                 IBlogCategoryService blogCategoryService) {
        this.blogArticleService = blogArticleService;
        this.blogArticleTagService = blogArticleTagService;
        this.blogCategoryService = blogCategoryService;
    }

    @GetMapping("/{id}")
    public Result<BlogArticle> getById(@PathVariable Long id) {
        BlogArticle article = blogArticleService.getById(id);
        return article == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(article);
    }

    @GetMapping("/page")
    public Result<Page<BlogArticle>> page(@RequestParam(required = false) Long current,
                                          @RequestParam(required = false) Long size,
                                          @RequestParam(required = false) Long page,
                                          @RequestParam(required = false) Long pageSize,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String title,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) String categoryName,
                                          @RequestParam(required = false) String status) {
        long resolvedCurrent = resolveCurrent(current, page);
        long resolvedSize = resolveSize(size, pageSize);
        validatePageRequest(resolvedCurrent, resolvedSize);

        if (!hasArticleListFilters(title, categoryId, categoryName, status)) {
            return Result.ok(blogArticleService.pageArticles(resolvedCurrent, resolvedSize, keyword));
        }

        String articleStatus = normalizeStatus(status);
        if (StringUtils.hasText(status) && !ARTICLE_STATUSES.contains(articleStatus)) {
            return Result.fail(ResultCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<BlogArticle> queryWrapper = new LambdaQueryWrapper<>();
        applyArticleSearch(queryWrapper, keyword, title);
        applyCategoryFilter(queryWrapper, categoryId, categoryName);
        if (StringUtils.hasText(articleStatus)) {
            queryWrapper.eq(BlogArticle::getArticleStatus, articleStatus);
        }
        queryWrapper.orderByDesc(BlogArticle::getPublishedTime, BlogArticle::getUpdateTime, BlogArticle::getId);

        Page<BlogArticle> articlePage = new Page<>(resolvedCurrent, resolvedSize);
        blogArticleService.page(articlePage, queryWrapper);
        return Result.ok(articlePage);
    }

    @GetMapping("/{id}/tags")
    public Result<List<BlogTag>> getTags(@PathVariable Long id) {
        BlogArticle article = blogArticleService.getById(id);
        return article == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(blogArticleTagService.listTagsByArticleId(id));
    }

    @PostMapping
    public Result<BlogArticle> create(@RequestBody BlogArticleCreateRequest request) {
        validateArticleForCreate(request);
        BlogArticle article = toBlogArticle(request);
        return Result.ok(blogArticleService.createArticle(article));
    }

    @PutMapping("/{id}/tags")
    public Result<List<BlogTag>> updateTags(@PathVariable Long id, @RequestBody ArticleTagUpdateRequest request) {
        validateArticleTagUpdateRequest(request);
        return Result.ok(blogArticleTagService.replaceArticleTags(id, request.getTagIds()));
    }

    @PutMapping("/{id}")
    public Result<BlogArticle> update(@PathVariable Long id, @RequestBody BlogArticleUpdateRequest request) {
        validateArticleForUpdate(request);
        BlogArticle article = toBlogArticle(request);
        BlogArticle updatedArticle = blogArticleService.updateArticle(id, article);
        return updatedArticle == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(updatedArticle);
    }

    @PutMapping("/{id}/status")
    public Result<BlogArticle> updateStatus(@PathVariable Long id, @RequestParam String status) {
        String normalizedStatus = normalizeStatus(status);
        validateStatus(normalizedStatus);
        BlogArticle updatedArticle = blogArticleService.updateArticleStatus(id, normalizedStatus);
        return updatedArticle == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(updatedArticle);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return blogArticleService.deleteArticle(id) ? Result.ok(null) : Result.fail(ResultCodeEnum.NOT_FOUND);
    }

    private long resolveCurrent(Long current, Long page) {
        return current != null ? current : page == null ? DEFAULT_CURRENT : page;
    }

    private long resolveSize(Long size, Long pageSize) {
        return size != null ? size : pageSize == null ? DEFAULT_SIZE : pageSize;
    }

    private boolean hasArticleListFilters(String title, Long categoryId, String categoryName, String status) {
        return StringUtils.hasText(title)
                || categoryId != null
                || StringUtils.hasText(categoryName)
                || StringUtils.hasText(status);
    }

    private void applyArticleSearch(LambdaQueryWrapper<BlogArticle> queryWrapper, String keyword, String title) {
        if (StringUtils.hasText(title)) {
            queryWrapper.like(BlogArticle::getArticleTitle, title.trim());
            return;
        }

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(BlogArticle::getArticleTitle, keyword.trim())
                    .or()
                    .like(BlogArticle::getArticleSummary, keyword.trim()));
        }
    }

    private void applyCategoryFilter(LambdaQueryWrapper<BlogArticle> queryWrapper,
                                     Long categoryId,
                                     String categoryName) {
        if (categoryId != null) {
            queryWrapper.eq(BlogArticle::getCategoryId, categoryId);
            return;
        }

        if (StringUtils.hasText(categoryName)) {
            List<Long> categoryIds = blogCategoryService.list(new LambdaQueryWrapper<BlogCategory>()
                            .like(BlogCategory::getCategoryName, categoryName.trim()))
                    .stream()
                    .map(BlogCategory::getId)
                    .toList();
            if (categoryIds.isEmpty()) {
                queryWrapper.eq(BlogArticle::getId, -1L);
            } else {
                queryWrapper.in(BlogArticle::getCategoryId, categoryIds);
            }
        }
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

    private void validateArticleForCreate(BlogArticleCreateRequest request) {
        validateArticle(request);
    }

    private void validateArticleForUpdate(BlogArticleUpdateRequest request) {
        validateArticle(request);
    }

    private void validateArticle(BlogArticleCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getArticleTitle())) {
            throw new IllegalArgumentException("articleTitle must not be blank");
        }
        if (!StringUtils.hasText(request.getArticleSlug())) {
            throw new IllegalArgumentException("articleSlug must not be blank");
        }
        if (!StringUtils.hasText(request.getArticleContent())) {
            throw new IllegalArgumentException("articleContent must not be blank");
        }
    }

    private void validateArticle(BlogArticleUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getArticleTitle())) {
            throw new IllegalArgumentException("articleTitle must not be blank");
        }
        if (!StringUtils.hasText(request.getArticleSlug())) {
            throw new IllegalArgumentException("articleSlug must not be blank");
        }
        if (!StringUtils.hasText(request.getArticleContent())) {
            throw new IllegalArgumentException("articleContent must not be blank");
        }
    }

    private BlogArticle toBlogArticle(BlogArticleCreateRequest request) {
        BlogArticle article = new BlogArticle();
        populateWritableFields(article,
                request.getArticleTitle(),
                request.getArticleSlug(),
                request.getArticleSummary(),
                request.getCoverUrl(),
                request.getArticleContent(),
                request.getCategoryId(),
                request.getPublishTime(),
                request.getTopFlag(),
                request.getAllowComment());
        article.setArticleStatus(request.getArticleStatus());
        return article;
    }

    private BlogArticle toBlogArticle(BlogArticleUpdateRequest request) {
        BlogArticle article = new BlogArticle();
        populateWritableFields(article,
                request.getArticleTitle(),
                request.getArticleSlug(),
                request.getArticleSummary(),
                request.getCoverUrl(),
                request.getArticleContent(),
                request.getCategoryId(),
                request.getPublishTime(),
                request.getTopFlag(),
                request.getAllowComment());
        article.setArticleStatus(request.getArticleStatus());
        return article;
    }

    private void populateWritableFields(BlogArticle article,
                                        String articleTitle,
                                        String articleSlug,
                                        String articleSummary,
                                        String coverUrl,
                                        String articleContent,
                                        Long categoryId,
                                        LocalDateTime publishTime,
                                        Boolean topFlag,
                                        Boolean allowComment) {
        article.setArticleTitle(articleTitle);
        article.setArticleSlug(articleSlug);
        article.setArticleSummary(articleSummary);
        article.setCoverUrl(coverUrl);
        article.setArticleContent(articleContent);
        article.setCategoryId(categoryId);
        article.setPublishTime(publishTime);
        article.setTopFlag(topFlag);
        article.setAllowComment(allowComment);
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "";
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "OFFLINE".equals(normalized) ? "PRIVATE" : normalized;
    }

    private void validateStatus(String status) {
        if (!ARTICLE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("status must be one of DRAFT, PUBLISHED, PRIVATE");
        }
    }

    private void validateArticleTagUpdateRequest(ArticleTagUpdateRequest request) {
        if (request == null || request.getTagIds() == null) {
            throw new IllegalArgumentException("tagIds must not be null");
        }
    }
}
