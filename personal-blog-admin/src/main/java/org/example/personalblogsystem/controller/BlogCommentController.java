package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.entity.BlogComment;
import org.example.personalblogsystem.service.IBlogCommentService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/admin/comment")
public class BlogCommentController {

    private static final long MAX_PAGE_SIZE = 100L;

    private final IBlogCommentService blogCommentService;

    public BlogCommentController(IBlogCommentService blogCommentService) {
        this.blogCommentService = blogCommentService;
    }

    @GetMapping("/page")
    public Result<Page<BlogComment>> page(@RequestParam long current,
                                          @RequestParam long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long articleId) {
        validatePageRequest(current, size);
        validateStatusIfPresent(status);
        return Result.ok(blogCommentService.pageComments(current, size, keyword, status, articleId));
    }

    @GetMapping("/article/{articleId}")
    public Result<List<BlogComment>> listByArticleId(@PathVariable Long articleId) {
        return Result.ok(blogCommentService.listCommentsByArticleId(articleId));
    }

    @PutMapping("/{id}/status")
    public Result<BlogComment> updateStatus(@PathVariable Long id, @RequestParam String status) {
        validateStatus(status);
        BlogComment updatedComment = blogCommentService.updateCommentStatus(id, status);
        return updatedComment == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(updatedComment);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return blogCommentService.deleteComment(id) ? Result.ok(null) : Result.fail(ResultCodeEnum.NOT_FOUND);
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

    private void validateStatusIfPresent(String status) {
        if (StringUtils.hasText(status)) {
            validateStatus(status);
        }
    }

    private void validateStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("status must be one of PENDING, APPROVED, REJECTED");
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(normalized) && !"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new IllegalArgumentException("status must be one of PENDING, APPROVED, REJECTED");
        }
    }
}
