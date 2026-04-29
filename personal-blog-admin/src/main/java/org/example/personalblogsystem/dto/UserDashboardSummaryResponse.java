package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserDashboardSummaryResponse {

    private long articleCount;
    private long publishedCount;
    private long draftCount;
    private long privateCount;
    private long totalViewCount;
    private long monthUpdateCount;
    private long pendingCommentCount;
    private List<UserArticleResponse> recentArticles;
}
