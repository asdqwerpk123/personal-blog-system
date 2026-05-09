package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PublicArticleDetailResponse extends PublicArticleResponse {

    private String articleSlug;
    private String articleContent;
    private Boolean allowComment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
