package org.example.personalblogsystem.service;

import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.dto.LoginUserResponse;

public interface IAuthService {

    LoginUserResponse login(LoginRequest request);
}
