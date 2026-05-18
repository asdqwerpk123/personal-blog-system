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
     * Course login alias that reuses the existing admin login business flow.
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

    /**
     * Return current authenticated user information without password_hash.
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
     * Return all active users for administrators.
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
