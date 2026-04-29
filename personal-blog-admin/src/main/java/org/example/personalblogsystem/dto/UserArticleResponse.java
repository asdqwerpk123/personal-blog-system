package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UserArticleResponse {

    private Long id;
    private String articleTitle;
    private String articleSlug;
    private String articleSummary;
    private String articleContent;
    private Long authorId;
    private String authorName;
    private Long categoryId;
    private String categoryName;
    private String articleStatus;
    private Boolean topFlag;
    private Boolean allowComment;
    private Integer viewCount;
    private LocalDateTime publishedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<String> tags;
    private List<Long> tagIds;
}
