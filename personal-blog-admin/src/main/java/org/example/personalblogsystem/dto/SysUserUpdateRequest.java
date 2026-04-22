package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SysUserUpdateRequest {

    private String nickName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String introduction;
    private Long roleId;
}
