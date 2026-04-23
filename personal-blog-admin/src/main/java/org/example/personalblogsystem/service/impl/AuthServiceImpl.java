package org.example.personalblogsystem.service.impl;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.auth.JwtTokenService;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.dto.LoginUserQueryRow;
import org.example.personalblogsystem.dto.LoginUserResponse;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.IAuthService;
import org.example.personalblogsystem.service.OperationLogRecordService;
import org.example.personalblogsystem.service.PasswordHashService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class AuthServiceImpl implements IAuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtTokenService jwtTokenService;
    private final PasswordHashService passwordHashService;
    private final OperationLogRecordService operationLogRecordService;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           JwtTokenService jwtTokenService,
                           PasswordHashService passwordHashService,
                           OperationLogRecordService operationLogRecordService) {
        this.sysUserMapper = sysUserMapper;
        this.jwtTokenService = jwtTokenService;
        this.passwordHashService = passwordHashService;
        this.operationLogRecordService = operationLogRecordService;
    }

    @Override
    public LoginUserResponse login(LoginRequest request) {
        validateRequest(request);

        String userName = request.getUserName().trim();
        LoginUserQueryRow row = sysUserMapper.selectLoginUserByUserName(userName);
        if (row == null) {
            throw new IllegalArgumentException("username or password is incorrect");
        }
        if (!isEnabled(row.getUserStatus()) || !passwordHashService.matches(request.getPassword(), row.getPasswordHash())) {
            operationLogRecordService.recordFailure(row.getId(), "AUTH", row.getId(), "LOGIN", "Login failed: " + row.getUserName());
            throw new IllegalArgumentException("username or password is incorrect");
        }
        if (!isAdminRole(row.getRoleCode())) {
            operationLogRecordService.recordFailure(row.getId(), "AUTH", row.getId(), "LOGIN", "Login denied: " + row.getUserName());
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }

        LoginUserResponse response = toResponse(row);
        operationLogRecordService.recordSuccess(row.getId(), "AUTH", row.getId(), "LOGIN", "Login success: " + row.getUserName());
        return response;
    }

    private void validateRequest(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUserName())) {
            throw new IllegalArgumentException("userName must not be blank");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("password must not be blank");
        }
    }

    private boolean isEnabled(String userStatus) {
        return "ENABLED".equalsIgnoreCase(userStatus == null ? null : userStatus.trim());
    }

    private boolean isAdminRole(String roleCode) {
        return "SUPER_ADMIN".equalsIgnoreCase(roleCode) || "ADMIN".equalsIgnoreCase(roleCode);
    }

    private LoginUserResponse toResponse(LoginUserQueryRow row) {
        LoginUserResponse response = new LoginUserResponse();
        response.setId(row.getId());
        response.setUserName(row.getUserName());
        response.setNickName(row.getNickName());
        response.setEmail(row.getEmail());
        response.setPhone(row.getPhone());
        response.setAvatarUrl(row.getAvatarUrl());
        response.setIntroduction(row.getIntroduction());
        response.setRoleId(row.getRoleId());
        response.setRoleCode(row.getRoleCode());
        response.setRoleName(row.getRoleName());
        response.setUserStatus(row.getUserStatus());
        AdminAuthPrincipal principal = new AdminAuthPrincipal(row.getId(), row.getUserName(), row.getRoleId(), row.getRoleCode());
        Instant expiresAt = jwtTokenService.calculateAccessTokenExpiresAt();
        response.setAccessToken(jwtTokenService.issueAccessToken(principal, expiresAt));
        response.setTokenType("Bearer");
        response.setExpiresAt(expiresAt);
        return response;
    }
}
