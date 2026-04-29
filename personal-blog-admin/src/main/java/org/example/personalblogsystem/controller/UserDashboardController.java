package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.auth.UserAuthContext;
import org.example.personalblogsystem.dto.UserDashboardSummaryResponse;
import org.example.personalblogsystem.service.IBlogArticleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/dashboard")
public class UserDashboardController {

    private final IBlogArticleService blogArticleService;

    public UserDashboardController(IBlogArticleService blogArticleService) {
        this.blogArticleService = blogArticleService;
    }

    @GetMapping("/summary")
    public Result<UserDashboardSummaryResponse> summary() {
        Long userId = UserAuthContext.requireCurrentUser().getUserId();
        return Result.ok(blogArticleService.getUserDashboardSummary(userId));
    }
}
