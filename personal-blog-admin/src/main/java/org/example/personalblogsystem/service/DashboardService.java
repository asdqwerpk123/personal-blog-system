package org.example.personalblogsystem.service;

import org.example.personalblogsystem.dto.DashboardSummaryResponse;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogComment;
import org.example.personalblogsystem.entity.SysOperationLog;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final IBlogArticleService blogArticleService;
    private final IBlogCategoryService blogCategoryService;
    private final IBlogTagService blogTagService;
    private final IBlogCommentService blogCommentService;
    private final IBlogFriendLinkService blogFriendLinkService;
    private final ISysOperationLogService sysOperationLogService;

    public DashboardService(IBlogArticleService blogArticleService,
                            IBlogCategoryService blogCategoryService,
                            IBlogTagService blogTagService,
                            IBlogCommentService blogCommentService,
                            IBlogFriendLinkService blogFriendLinkService,
                            ISysOperationLogService sysOperationLogService) {
        this.blogArticleService = blogArticleService;
        this.blogCategoryService = blogCategoryService;
        this.blogTagService = blogTagService;
        this.blogCommentService = blogCommentService;
        this.blogFriendLinkService = blogFriendLinkService;
        this.sysOperationLogService = sysOperationLogService;
    }

    public DashboardSummaryResponse getSummary() {
        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setArticleCount(blogArticleService.count());
        response.setCategoryCount(blogCategoryService.count());
        response.setTagCount(blogTagService.count());
        response.setCommentCount(blogCommentService.count());
        response.setPendingCommentCount(blogCommentService.lambdaQuery()
                .eq(BlogComment::getCommentStatus, "PENDING")
                .count());
        response.setFriendLinkCount(blogFriendLinkService.count());
        response.setLatestArticles(blogArticleService.lambdaQuery()
                .orderByDesc(BlogArticle::getUpdateTime, BlogArticle::getId)
                .last("limit 5")
                .list()
                .stream()
                .map(this::toArticleItem)
                .toList());
        response.setLatestComments(blogCommentService.lambdaQuery()
                .orderByDesc(BlogComment::getUpdateTime, BlogComment::getId)
                .last("limit 5")
                .list()
                .stream()
                .map(this::toCommentItem)
                .toList());
        response.setLatestLogs(sysOperationLogService.lambdaQuery()
                .orderByDesc(SysOperationLog::getCreateTime, SysOperationLog::getId)
                .last("limit 5")
                .list()
                .stream()
                .map(this::toLogItem)
                .toList());
        return response;
    }

    private DashboardSummaryResponse.ArticleItem toArticleItem(BlogArticle article) {
        DashboardSummaryResponse.ArticleItem item = new DashboardSummaryResponse.ArticleItem();
        item.setId(article.getId());
        item.setArticleTitle(article.getArticleTitle());
        item.setCategoryId(article.getCategoryId());
        item.setArticleStatus(article.getArticleStatus());
        item.setViewCount(article.getViewCount());
        item.setUpdateTime(article.getUpdateTime());
        return item;
    }

    private DashboardSummaryResponse.CommentItem toCommentItem(BlogComment comment) {
        DashboardSummaryResponse.CommentItem item = new DashboardSummaryResponse.CommentItem();
        item.setId(comment.getId());
        item.setArticleId(comment.getArticleId());
        item.setUserId(comment.getUserId());
        item.setCommentContent(comment.getCommentContent());
        item.setCommentStatus(comment.getCommentStatus());
        item.setCreateTime(comment.getCreateTime());
        item.setUpdateTime(comment.getUpdateTime());
        return item;
    }

    private DashboardSummaryResponse.LogItem toLogItem(SysOperationLog operationLog) {
        DashboardSummaryResponse.LogItem item = new DashboardSummaryResponse.LogItem();
        item.setId(operationLog.getId());
        item.setOperatorUserId(operationLog.getOperatorUserId());
        item.setTargetType(operationLog.getTargetType());
        item.setTargetId(operationLog.getTargetId());
        item.setActionType(operationLog.getActionType());
        item.setActionResult(operationLog.getActionResult());
        item.setActionDetail(operationLog.getActionDetail());
        item.setCreateTime(operationLog.getCreateTime());
        return item;
    }
}
