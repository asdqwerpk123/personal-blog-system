package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PublicCommentResponse {

    private Long id;
    private Long articleId;
    private Long parentId;
    private Long userId;
    private String nickName;
    private String avatarUrl;
    private String commentContent;
    private LocalDateTime createTime;
}
