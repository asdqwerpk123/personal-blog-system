package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogFriendLinkCreateRequest {

    private String siteName;
    private String siteUrl;
    private String siteLogo;
    private String siteDesc;
    private String ownerName;
    private String contactEmail;
    private String linkStatus;
}
