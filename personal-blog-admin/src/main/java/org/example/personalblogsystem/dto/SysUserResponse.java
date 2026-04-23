package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SysUserResponse {

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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
