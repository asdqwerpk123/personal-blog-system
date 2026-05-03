package org.example.personalblogsystem.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "blog.upload")
public class AdminUploadProperties {

    private String friendLinkLogoDir = "uploads/friend-links";
    private String avatarDir = "uploads/avatars";
    private String articleCoverDir = "uploads/article-covers";
}
