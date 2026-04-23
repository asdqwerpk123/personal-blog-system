package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class LoginUserResponse {

    private Long id;
    private String userName;
    private String nickName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String introduction;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String userStatus;
    private String accessToken;
    private String tokenType;
    private Instant expiresAt;
}
