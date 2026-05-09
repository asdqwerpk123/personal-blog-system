package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicTagResponse {

    private Long id;
    private String tagName;
    private String description;
}
