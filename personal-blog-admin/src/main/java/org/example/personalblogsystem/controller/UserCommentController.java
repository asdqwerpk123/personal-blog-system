package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.auth.UserAuthContext;
import org.example.personalblogsystem.dto.UserCommentCreateRequest;
import org.example.personalblogsystem.dto.UserCommentResponse;
import org.example.personalblogsystem.service.IBlogCommentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/comments")
public class UserCommentController {

    private static final long DEFAULT_CURRENT = 1L;
    private static final long DEFAULT_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final IBlogCommentService blogCommentService;

    public UserCommentController(IBlogCommentService blogCommentService) {
        this.blogCommentService = blogCommentService;
    }

    @GetMapping("/page")
    public Result<Page<UserCommentResponse>> page(@RequestParam(required = false) Long current,
                                                  @RequestParam(required = false) Long size,
                                                  @RequestParam(required = false) Long page,
                                                  @RequestParam(required = false) Long pageSize,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status) {
        long resolvedCurrent = resolveCurrent(current, page);
        long resolvedSize = resolveSize(size, pageSize);
        validatePageRequest(resolvedCurrent, resolvedSize);
        Long userId = UserAuthContext.requireCurrentUser().getUserId();
        return Result.ok(blogCommentService.pageUserComments(resolvedCurrent, resolvedSize, userId, keyword, status));
    }

    @PostMapping
    public Result<UserCommentResponse> create(@RequestBody UserCommentCreateRequest request) {
        Long userId = UserAuthContext.requireCurrentUser().getUserId();
        return Result.ok(blogCommentService.createUserComment(userId, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserAuthContext.requireCurrentUser().getUserId();
        blogCommentService.deleteUserComment(userId, id);
        return Result.ok(null);
    }

    private long resolveCurrent(Long current, Long page) {
        return current != null ? current : page == null ? DEFAULT_CURRENT : page;
    }

    private long resolveSize(Long size, Long pageSize) {
        return size != null ? size : pageSize == null ? DEFAULT_SIZE : pageSize;
    }

    private void validatePageRequest(long current, long size) {
        if (current <= 0) {
            throw new IllegalArgumentException("页码必须大于 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("每页数量必须大于 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页数量不能超过 100");
        }
    }
}
