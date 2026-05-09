package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicCategoryResponse {

    private Long id;
    private String categoryName;
    private String description;
    private Integer sortNo;
}
