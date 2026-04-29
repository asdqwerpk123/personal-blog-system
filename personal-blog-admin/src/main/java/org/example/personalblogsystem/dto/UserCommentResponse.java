package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserCommentResponse {

    private Long id;
    private Long articleId;
    private String articleTitle;
    private Long userId;
    private String nickName;
    private String commentContent;
    private String commentStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
