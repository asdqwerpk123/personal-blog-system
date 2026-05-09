package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicFriendLinkResponse {

    private Long id;
    private String siteName;
    private String siteUrl;
    private String siteLogo;
    private String siteDesc;
    private String ownerName;
}
