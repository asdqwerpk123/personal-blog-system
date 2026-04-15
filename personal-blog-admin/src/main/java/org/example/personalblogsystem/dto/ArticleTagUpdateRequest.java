package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleTagUpdateRequest {

    private List<Long> tagIds;
}
