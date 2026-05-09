package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.dto.PublicArticleDetailResponse;
import org.example.personalblogsystem.dto.PublicArticleResponse;
import org.example.personalblogsystem.dto.PublicCommentResponse;
import org.example.personalblogsystem.service.IBlogArticleService;
import org.example.personalblogsystem.service.IBlogCommentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/articles")
public class PublicArticleController {

    private static final long DEFAULT_CURRENT = 1L;
    private static final long DEFAULT_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final IBlogArticleService blogArticleService;
    private final IBlogCommentService blogCommentService;

    public PublicArticleController(IBlogArticleService blogArticleService,
                                   IBlogCommentService blogCommentService) {
        this.blogArticleService = blogArticleService;
        this.blogCommentService = blogCommentService;
    }

    @GetMapping("/page")
    public Result<Page<PublicArticleResponse>> page(@RequestParam(required = false) Long current,
                                                    @RequestParam(required = false) Long size,
                                                    @RequestParam(required = false) Long page,
                                                    @RequestParam(required = false) Long pageSize,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String title,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) Long tagId) {
        long resolvedCurrent = resolveCurrent(current, page);
        long resolvedSize = resolveSize(size, pageSize);
        validatePageRequest(resolvedCurrent, resolvedSize);
        String searchKeyword = keyword != null && !keyword.isBlank() ? keyword : title;
        return Result.ok(blogArticleService.pagePublicArticles(
                resolvedCurrent,
                resolvedSize,
                searchKeyword,
                categoryId,
                tagId));
    }

    @GetMapping("/{id}")
    public Result<PublicArticleDetailResponse> detail(@PathVariable Long id) {
        return Result.ok(blogArticleService.getPublicArticle(id));
    }

    @GetMapping("/{id}/comments")
    public Result<Page<PublicCommentResponse>> comments(@PathVariable Long id,
                                                        @RequestParam(required = false) Long current,
                                                        @RequestParam(required = false) Long size,
                                                        @RequestParam(required = false) Long page,
                                                        @RequestParam(required = false) Long pageSize) {
        long resolvedCurrent = resolveCurrent(current, page);
        long resolvedSize = resolveSize(size, pageSize);
        validatePageRequest(resolvedCurrent, resolvedSize);
        return Result.ok(blogCommentService.pagePublicCommentsByArticleId(id, resolvedCurrent, resolvedSize));
    }

    private long resolveCurrent(Long current, Long page) {
        return current != null ? current : page == null ? DEFAULT_CURRENT : page;
    }

    private long resolveSize(Long size, Long pageSize) {
        return size != null ? size : pageSize == null ? DEFAULT_SIZE : pageSize;
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
}
