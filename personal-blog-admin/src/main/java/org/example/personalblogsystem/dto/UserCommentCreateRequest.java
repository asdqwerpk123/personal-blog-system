package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCommentCreateRequest {

    private Long articleId;
    private String commentContent;
}
