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

/**
 * 认证服务实现类，负责账号密码认证、JWT 签发、Redis 登录态缓存和登录操作日志记录。
 * 通过 AuthenticationManager 接入 Spring Security，并按角色区分后台登录与用户端登录。
 */
@Service
public class AuthServiceImpl implements IAuthService {

    /**
     * 登录凭据校验失败时返回给前端的统一提示，避免暴露账号是否存在。
     */
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

    /**
     * 后台登录入口，允许管理员、超级管理员和普通用户通过统一登录接口认证。
     *
     * @param request 登录请求，用户名和密码不能为空
     * @return 登录成功后的用户资料、访问令牌和过期时间
     * @throws IllegalArgumentException 凭据错误或请求字段为空时抛出
     * @throws BlogException 角色不被允许或认证主体无效时抛出
     */
    @Override
    public LoginUserResponse login(LoginRequest request) {
        return loginInternal(request, this::isSupportedRole);
    }

    /**
     * 用户端登录入口，仅允许 USER 角色账号登录。
     *
     * @param request 登录请求，用户名和密码不能为空
     * @return 登录成功后的用户资料、访问令牌和过期时间
     * @throws IllegalArgumentException 凭据错误或请求字段为空时抛出
     * @throws BlogException 非 USER 角色尝试登录用户端时抛出
     */
    @Override
    public LoginUserResponse loginUser(LoginRequest request) {
        return loginInternal(request, this::isUserRole);
    }

    /**
     * 退出登录，删除 Redis 中的登录态并清空当前线程安全上下文。
     *
     * @throws BlogException 当前请求没有有效 LoginUser 主体时抛出未认证异常
     */
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

    /**
     * 登录模板流程：参数校验、Spring Security 认证、角色校验、签发令牌、缓存登录态并写入操作日志。
     */
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

    /**
     * 注册普通用户账号。
     *
     * @param request 注册请求
     * @return 去除密码散列后的用户资料
     */
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
