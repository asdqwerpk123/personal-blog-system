package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SysUserCreateRequest {

    private String userName;
    private String password;
    private String nickName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String introduction;
    private Long roleId;
    private String userStatus;
}
