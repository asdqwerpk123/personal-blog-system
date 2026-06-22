package org.example.personalblogsystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlogArticleUpdateRequest {

    private String articleTitle;
    private String articleSlug;
    private String articleSummary;
    private String coverUrl;
    private String articleContent;
    private Long categoryId;
    private String articleStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    private Boolean topFlag;
    private Boolean allowComment;
}
