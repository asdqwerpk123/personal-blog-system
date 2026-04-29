package org.example.personalblogsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserArticleRequest {

    private String articleTitle;
    private String articleSlug;
    private String articleSummary;
    private String articleContent;
    private Long categoryId;
    private String articleStatus;
    private List<Long> tagIds;
}
