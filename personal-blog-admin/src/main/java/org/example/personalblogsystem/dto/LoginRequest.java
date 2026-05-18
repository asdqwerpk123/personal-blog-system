package org.example.personalblogsystem.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @JsonAlias("username")
    private String userName;
    private String password;
}
