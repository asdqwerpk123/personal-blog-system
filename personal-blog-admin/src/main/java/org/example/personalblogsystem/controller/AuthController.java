package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.dto.LoginUserResponse;
import org.example.personalblogsystem.dto.SysUserResponse;
import org.example.personalblogsystem.dto.UserRegisterRequest;
import org.example.personalblogsystem.entity.SysRole;
import org.example.personalblogsystem.entity.SysUser;
import org.example.personalblogsystem.mapper.SysRoleMapper;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.auth.LoginUser;
import org.example.personalblogsystem.service.IAuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 认证授权控制器，提供后台管理员登录、用户登录注册、退出登录和当前用户信息查询接口。
 * 依赖 IAuthService 完成认证主流程，并通过 Spring Security 权限注解限制后台用户列表访问。
 */
@RestController
public class AuthController {

    private final IAuthService authService;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    public AuthController(IAuthService authService, SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper) {
        this.authService = authService;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
    }


    /**
     * 课程设计兼容登录接口，返回简化的 token 字段结构。
     *
     * @param request 登录请求，userName 和 password 必须有效
     * @return 仅包含 accessToken 与 tokenType 的登录结果
     * @throws IllegalArgumentException 用户名或密码为空、凭据错误时抛出
     * @throws BlogException 账号角色不允许登录时抛出未认证异常
     */
    @SecurityRequirements
    @Operation(summary = "login", description = "Course login alias for /admin/auth/login.", security = {})
    @PostMapping("/login")
    public Result<Map<String, String>> courseLogin(@RequestBody LoginRequest request) {
        LoginUserResponse response = authService.login(request);
        return Result.ok(Map.of(
                "accessToken", response.getAccessToken(),
                "tokenType", response.getTokenType()
        ));
    }

    /**
     * 后台管理员登录接口，校验账号密码并签发 Bearer 访问令牌。
     *
     * @param request 登录请求，支持管理员、超级管理员和普通用户角色
     * @return 包含用户资料、访问令牌和过期时间的登录响应
     * @throws IllegalArgumentException 用户名或密码为空、凭据错误时抛出
     * @throws BlogException 认证主体无效时抛出
     */
    @SecurityRequirements
    @Operation(summary = "admin login", description = "Validate credentials and return a Bearer access token.", security = {})
    @PostMapping("/admin/auth/login")
    public Result<LoginUserResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /**
     * 用户端登录接口，仅允许 USER 角色获取用户端访问令牌。
     *
     * @param request 登录请求，账号必须属于普通用户角色
     * @return 包含用户资料、访问令牌和过期时间的登录响应
     * @throws IllegalArgumentException 用户名或密码为空、凭据错误时抛出
     * @throws BlogException 非 USER 角色登录用户端时抛出未认证异常
     */
    @SecurityRequirements
    @Operation(summary = "user login", description = "Only USER role accounts can obtain a user access token.", security = {})
    @PostMapping("/user/auth/login")
    public Result<LoginUserResponse> userLogin(@RequestBody LoginRequest request) {
        return Result.ok(authService.loginUser(request));
    }

    /**
     * 后台退出登录，删除当前登录用户在 Redis 中的登录态。
     *
     * @return 空结果，表示退出操作完成
     * @throws BlogException 当前请求未携带有效登录主体时抛出未认证异常
     */
    @Operation(summary = "admin logout", description = "Remove current admin login state from Redis.")
    @PostMapping("/admin/auth/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok(null);
    }

    /**
     * 用户端退出登录，复用统一退出逻辑清理 Redis 登录态和安全上下文。
     *
     * @return 空结果，表示退出操作完成
     * @throws BlogException 当前请求未携带有效登录主体时抛出未认证异常
     */
    @Operation(summary = "user logout", description = "Remove current user login state from Redis.")
    @PostMapping("/user/auth/logout")
    public Result<Void> userLogout() {
        authService.logout();
        return Result.ok(null);
    }

    /**
     * 注册普通用户账号。
     *
     * @param request 注册请求，包含用户名、密码及用户资料
     * @return 去除密码散列后的用户资料
     * @throws IllegalArgumentException 注册参数不合法时抛出
     * @throws BlogException 用户名冲突等业务校验失败时抛出
     */
    @SecurityRequirements
    @Operation(summary = "user register", description = "Create a normal USER account.", security = {})
    @PostMapping("/user/auth/register")
    public Result<SysUserResponse> register(@RequestBody UserRegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    /**
     * 查询当前已认证用户信息，不返回 password_hash 等敏感字段。
     *
     * @param authentication Spring Security 当前认证对象
     * @return 当前用户资料及角色信息
     * @throws BlogException 当前请求未认证或用户不存在时抛出未认证异常
     */
    @Operation(summary = "current user info", description = "Return current authenticated user information.")
    @GetMapping("/user/info")
    public Result<SysUserResponse> userInfo(Authentication authentication) {
        String userName = resolveCurrentUserName(authentication);
        SysUser user = selectActiveUserByUserName(userName);
        SysRole role = selectActiveRole(user.getRoleId());
        return Result.ok(toResponse(user, role));
    }


    /**
     * 查询后台用户列表，仅管理员和超级管理员可访问。
     *
     * @return 所有未删除用户的脱敏资料列表
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "admin user list", description = "Return all active users without password_hash.")
    @GetMapping("/admin/list")
    public Result<List<SysUserResponse>> adminList() {
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, false)
                .orderByAsc(SysUser::getId));
        Map<Long, SysRole> roles = selectRoles(users);
        return Result.ok(users.stream()
                .map(user -> toResponse(user, roles.get(user.getRoleId())))
                .toList());
    }

    private String resolveCurrentUserName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser.getUserName();
        }
        String name = authentication.getName();
        if (!StringUtils.hasText(name)) {
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }
        return name;
    }

    private SysUser selectActiveUserByUserName(String userName) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, userName)
                .eq(SysUser::getDeleted, false)
                .last("limit 1"));
        if (user == null) {
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }
        return user;
    }

    private SysRole selectActiveRole(Long roleId) {
        if (roleId == null) {
            return null;
        }
        return sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getId, roleId)
                .eq(SysRole::getDeleted, false)
                .last("limit 1"));
    }

    private Map<Long, SysRole> selectRoles(List<SysUser> users) {
        Set<Long> roleIds = users.stream()
                .map(SysUser::getRoleId)
                .filter(roleId -> roleId != null)
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getDeleted, false))
                .stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));
    }

    private SysUserResponse toResponse(SysUser user, SysRole role) {
        SysUserResponse response = new SysUserResponse();
        response.setId(user.getId());
        response.setUserName(user.getUserName());
        response.setNickName(user.getNickName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setIntroduction(user.getIntroduction());
        response.setRoleId(user.getRoleId());
        response.setRoleCode(role == null ? null : role.getRoleCode());
        response.setRoleName(role == null ? null : role.getRoleName());
        response.setUserStatus(user.getUserStatus());
        response.setCreateTime(user.getCreateTime());
        response.setUpdateTime(user.getUpdateTime());
        return response;
    }
}
