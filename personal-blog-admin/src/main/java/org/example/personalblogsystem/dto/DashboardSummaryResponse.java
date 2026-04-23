package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DashboardSummaryResponse {

    private long articleCount;
    private long categoryCount;
    private long tagCount;
    private long commentCount;
    private long pendingCommentCount;
    private long friendLinkCount;
    private List<ArticleItem> latestArticles;
    private List<CommentItem> latestComments;
    private List<LogItem> latestLogs;

    @Getter
    @Setter
    public static class ArticleItem {
        private Long id;
        private String articleTitle;
        private Long categoryId;
        private String articleStatus;
        private Integer viewCount;
        private LocalDateTime updateTime;
    }

    @Getter
    @Setter
    public static class CommentItem {
        private Long id;
        private Long articleId;
        private Long userId;
        private String commentContent;
        private String commentStatus;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Getter
    @Setter
    public static class LogItem {
        private Long id;
        private Long operatorUserId;
        private String targetType;
        private Long targetId;
        private String actionType;
        private String actionResult;
        private String actionDetail;
        private LocalDateTime createTime;
    }
}
