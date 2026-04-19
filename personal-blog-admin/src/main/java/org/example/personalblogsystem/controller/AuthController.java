package org.example.personalblogsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.dto.LoginUserResponse;
import org.example.personalblogsystem.service.IAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
public class AuthController {

    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @SecurityRequirements
    @Operation(summary = "后台登录", description = "校验用户名密码并返回 Bearer access token。", security = {})
    @PostMapping("/login")
    public Result<LoginUserResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }
}
