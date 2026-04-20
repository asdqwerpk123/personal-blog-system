package org.example.personalblogsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlogArticleCreateRequest {

    private String articleTitle;
    private String articleSlug;
    private String articleSummary;
    private String coverUrl;
    private String articleContent;
    private Long categoryId;
    private String articleStatus;
    private Boolean topFlag;
    private Boolean allowComment;
}
