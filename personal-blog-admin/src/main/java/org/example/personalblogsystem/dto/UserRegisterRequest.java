package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterRequest {

    private String userName;
    private String password;
    private String nickName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String introduction;
}
