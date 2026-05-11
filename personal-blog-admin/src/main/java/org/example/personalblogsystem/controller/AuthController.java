package org.example.personalblogsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.dto.LoginUserResponse;
import org.example.personalblogsystem.dto.SysUserResponse;
import org.example.personalblogsystem.dto.UserRegisterRequest;
import org.example.personalblogsystem.service.IAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @SecurityRequirements
    @Operation(summary = "admin login", description = "Validate credentials and return a Bearer access token.", security = {})
    @PostMapping("/admin/auth/login")
    public Result<LoginUserResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @SecurityRequirements
    @Operation(summary = "user login", description = "Only USER role accounts can obtain a user access token.", security = {})
    @PostMapping("/user/auth/login")
    public Result<LoginUserResponse> userLogin(@RequestBody LoginRequest request) {
        return Result.ok(authService.loginUser(request));
    }

    @Operation(summary = "admin logout", description = "Remove current admin login state from Redis.")
    @PostMapping("/admin/auth/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok(null);
    }

    @Operation(summary = "user logout", description = "Remove current user login state from Redis.")
    @PostMapping("/user/auth/logout")
    public Result<Void> userLogout() {
        authService.logout();
        return Result.ok(null);
    }

    @SecurityRequirements
    @Operation(summary = "user register", description = "Create a normal USER account.", security = {})
    @PostMapping("/user/auth/register")
    public Result<SysUserResponse> register(@RequestBody UserRegisterRequest request) {
        return Result.ok(authService.register(request));
    }
}
