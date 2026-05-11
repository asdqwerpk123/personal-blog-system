package org.example.personalblogsystem.service.impl;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.auth.JwtTokenService;
import org.example.personalblogsystem.auth.LoginUser;
import org.example.personalblogsystem.auth.UserAuthContext;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.dto.LoginUserQueryRow;
import org.example.personalblogsystem.dto.LoginUserResponse;
import org.example.personalblogsystem.dto.SysUserResponse;
import org.example.personalblogsystem.dto.UserRegisterRequest;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.IAuthService;
import org.example.personalblogsystem.service.ISysUserService;
import org.example.personalblogsystem.service.OperationLogRecordService;
import org.example.personalblogsystem.utils.RedisCache;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Service
public class AuthServiceImpl implements IAuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "用户名或密码错误";

    private final SysUserMapper sysUserMapper;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;
    private final RedisCache redisCache;
    private final OperationLogRecordService operationLogRecordService;
    private final ISysUserService sysUserService;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           JwtTokenService jwtTokenService,
                           AuthenticationManager authenticationManager,
                           RedisCache redisCache,
                           OperationLogRecordService operationLogRecordService,
                           ISysUserService sysUserService) {
        this.sysUserMapper = sysUserMapper;
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
        this.redisCache = redisCache;
        this.operationLogRecordService = operationLogRecordService;
        this.sysUserService = sysUserService;
    }

    @Override
    public LoginUserResponse login(LoginRequest request) {
        return loginInternal(request, this::isSupportedRole);
    }

    @Override
    public LoginUserResponse loginUser(LoginRequest request) {
        return loginInternal(request, this::isUserRole);
    }

    @Override
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }

        redisCache.deleteObject(LoginUser.redisKey(loginUser.getId()));
        SecurityContextHolder.clearContext();
        AdminAuthContext.clear();
        UserAuthContext.clear();
    }

    private LoginUserResponse loginInternal(LoginRequest request, Predicate<String> rolePredicate) {
        validateRequest(request);

        String userName = request.getUserName().trim();
        LoginUser loginUser = authenticate(userName, request.getPassword());
        if (!rolePredicate.test(loginUser.getRoleCode())) {
            operationLogRecordService.recordFailure(loginUser.getId(), "AUTH", loginUser.getId(),
                    "LOGIN_FAILURE", "login failed: invalid role " + loginUser.getUserName());
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }

        LoginUserResponse response = toResponse(loginUser);
        cacheLoginUser(loginUser, response.getExpiresAt());
        operationLogRecordService.recordSuccess(loginUser.getId(), "AUTH", loginUser.getId(),
                "LOGIN_SUCCESS", "login success: " + loginUser.getUserName());
        return response;
    }

    @Override
    public SysUserResponse register(UserRegisterRequest request) {
        return sysUserService.registerUser(request);
    }

    private LoginUser authenticate(String userName, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userName, password));
            return (LoginUser) authentication.getPrincipal();
        } catch (AuthenticationException exception) {
            recordLoginFailure(userName);
            throw new IllegalArgumentException(INVALID_CREDENTIALS_MESSAGE);
        }
    }

    private void recordLoginFailure(String userName) {
        LoginUserQueryRow row = sysUserMapper.selectLoginUserByUserName(userName);
        if (row == null) {
            operationLogRecordService.recordFailure(null, "AUTH", null, "LOGIN_FAILURE", "login failed: " + userName);
            return;
        }
        operationLogRecordService.recordFailure(row.getId(), "AUTH", row.getId(),
                "LOGIN_FAILURE", "login failed: " + row.getUserName());
    }

    private void cacheLoginUser(LoginUser loginUser, Instant expiresAt) {
        long ttlSeconds = Math.max(1, Duration.between(Instant.now(), expiresAt).getSeconds());
        redisCache.setCacheObject(loginUser.redisKey(), loginUser, ttlSeconds, TimeUnit.SECONDS);
    }

    private void validateRequest(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUserName())) {
            throw new IllegalArgumentException("userName must not be blank");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("password must not be blank");
        }
    }

    private boolean isSupportedRole(String roleCode) {
        return "SUPER_ADMIN".equalsIgnoreCase(roleCode)
                || "ADMIN".equalsIgnoreCase(roleCode)
                || "USER".equalsIgnoreCase(roleCode);
    }

    private boolean isUserRole(String roleCode) {
        return "USER".equalsIgnoreCase(roleCode);
    }

    private LoginUserResponse toResponse(LoginUser loginUser) {
        LoginUserResponse response = new LoginUserResponse();
        response.setId(loginUser.getId());
        response.setUserName(loginUser.getUserName());
        response.setNickName(loginUser.getNickName());
        response.setEmail(loginUser.getEmail());
        response.setPhone(loginUser.getPhone());
        response.setAvatarUrl(loginUser.getAvatarUrl());
        response.setIntroduction(loginUser.getIntroduction());
        response.setRoleId(loginUser.getRoleId());
        response.setRoleCode(loginUser.getRoleCode());
        response.setRoleName(loginUser.getRoleName());
        response.setUserStatus(loginUser.getUserStatus());

        AdminAuthPrincipal principal = loginUser.toPrincipal();
        Instant expiresAt = jwtTokenService.calculateAccessTokenExpiresAt();
        response.setAccessToken(jwtTokenService.issueAccessToken(principal, expiresAt));
        response.setTokenType("Bearer");
        response.setExpiresAt(expiresAt);
        return response;
    }
}
