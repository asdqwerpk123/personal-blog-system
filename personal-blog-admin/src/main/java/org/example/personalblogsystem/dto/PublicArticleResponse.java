package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PublicArticleResponse {

    private Long id;
    private String articleTitle;
    private String articleSummary;
    private String coverUrl;
    private Long authorId;
    private String authorName;
    private String authorAvatarUrl;
    private Long categoryId;
    private String categoryName;
    private List<PublicTagResponse> tags;
    private Boolean topFlag;
    private Integer viewCount;
    private LocalDateTime publishedTime;
}
