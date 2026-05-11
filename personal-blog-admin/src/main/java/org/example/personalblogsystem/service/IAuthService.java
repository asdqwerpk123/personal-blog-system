package org.example.personalblogsystem.service;

import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.dto.LoginUserResponse;
import org.example.personalblogsystem.dto.SysUserResponse;
import org.example.personalblogsystem.dto.UserRegisterRequest;

public interface IAuthService {

    LoginUserResponse login(LoginRequest request);

    LoginUserResponse loginUser(LoginRequest request);

    void logout();

    SysUserResponse register(UserRegisterRequest request);
}
