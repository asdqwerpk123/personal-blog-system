package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogTagCreateRequest {

    private String tagName;
    private String description;
}
