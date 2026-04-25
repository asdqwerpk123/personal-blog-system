package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.dto.AdminPasswordChangeRequest;
import org.example.personalblogsystem.dto.AdminProfileUpdateRequest;
import org.example.personalblogsystem.dto.SysUserCreateRequest;
import org.example.personalblogsystem.dto.SysUserPasswordResetRequest;
import org.example.personalblogsystem.dto.SysUserResponse;
import org.example.personalblogsystem.dto.SysUserStatusRequest;
import org.example.personalblogsystem.dto.SysUserUpdateRequest;
import org.example.personalblogsystem.entity.SysRole;
import org.example.personalblogsystem.entity.SysUser;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.AdminPermissionService;
import org.example.personalblogsystem.service.ISysRoleService;
import org.example.personalblogsystem.service.ISysUserService;
import org.example.personalblogsystem.service.OperationLogRecordService;
import org.example.personalblogsystem.service.PasswordHashService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * <p>
 * 鐢ㄦ埛琛?鏈嶅姟瀹炵幇绫?
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final ISysRoleService sysRoleService;
    private final PasswordHashService passwordHashService;
    private final AdminPermissionService adminPermissionService;
    private final OperationLogRecordService operationLogRecordService;

    public SysUserServiceImpl(ISysRoleService sysRoleService,
                              PasswordHashService passwordHashService,
                              AdminPermissionService adminPermissionService,
                              OperationLogRecordService operationLogRecordService) {
        this.sysRoleService = sysRoleService;
        this.passwordHashService = passwordHashService;
        this.adminPermissionService = adminPermissionService;
        this.operationLogRecordService = operationLogRecordService;
    }

    @Override
    public SysUserResponse getUserById(Long id) {
        AdminAuthPrincipal current = AdminAuthContext.requireCurrentUser();
        SysUser user = getById(id);
        if (user != null) {
            adminPermissionService.requireCanViewUser(current, user, requireRole(user.getRoleId()));
        }
        return user == null ? null : toResponse(user);
    }

    @Override
    public Page<SysUserResponse> pageUsers(long current, long size, String keyword) {
        AdminAuthPrincipal currentUser = AdminAuthContext.requireCurrentUser();
        adminPermissionService.requireUserManagementAccess(currentUser);
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        List<Long> visibleRoleIds = visibleRoleIds(currentUser);
        queryWrapper.in(SysUser::getRoleId, visibleRoleIds);
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.and(wrapper -> wrapper.like(SysUser::getUserName, keyword)
                    .or()
                    .like(SysUser::getNickName, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword));
        }
        queryWrapper.orderByAsc(SysUser::getId);
        Page<SysUser> page = page(new Page<>(current, size), queryWrapper);
        Page<SysUserResponse> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        responsePage.setRecords(toResponseList(page.getRecords()));
        return responsePage;
    }

    @Override
    @Transactional
    public SysUserResponse createUser(SysUserCreateRequest request) {
        AdminAuthPrincipal current = AdminAuthContext.requireCurrentUser();
        String userName = requireText(request.getUserName(), "userName must not be blank");
        String password = requireText(request.getPassword(), "password must not be blank");
        String nickName = requireText(request.getNickName(), "nickName must not be blank");
        String email = requireText(request.getEmail(), "email must not be blank");
        String phone = requireText(request.getPhone(), "phone must not be blank");
        Long roleId = requireRoleId(request.getRoleId());
        SysRole role = requireRole(roleId);
        String userStatus = normalizeCreateStatus(request.getUserStatus());
        adminPermissionService.requireCanCreateUser(current, role);
        assertUniqueUserName(userName, null);
        assertUniqueEmail(email, null);
        assertUniquePhone(phone, null);

        SysUser user = new SysUser();
        user.setUserName(userName);
        user.setPasswordHash(passwordHashService.hash(password));
        user.setNickName(nickName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        user.setIntroduction(trimToNull(request.getIntroduction()));
        user.setRoleId(roleId);
        user.setUserStatus(userStatus);
        save(user);
        operationLogRecordService.recordSuccess("USER", user.getId(), "CREATE_USER", "新增用户：" + user.getUserName());
        return toResponse(user);
    }

    @Override
    @Transactional
    public SysUserResponse updateUser(Long id, SysUserUpdateRequest request) {
        AdminAuthPrincipal current = AdminAuthContext.requireCurrentUser();
        SysUser user = requireExistingUser(id);
        SysRole currentRole = requireRole(user.getRoleId());
        String nickName = requireText(request.getNickName(), "nickName must not be blank");
        String email = requireText(request.getEmail(), "email must not be blank");
        String phone = requireText(request.getPhone(), "phone must not be blank");
        Long roleId = requireRoleId(request.getRoleId());
        SysRole role = requireRole(roleId);
        adminPermissionService.requireCanUpdateUser(current, user, currentRole, role);
        assertUniqueUserName(user.getUserName(), user.getId());
        assertUniqueEmail(email, user.getId());
        assertUniquePhone(phone, user.getId());

        user.setNickName(nickName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        user.setIntroduction(trimToNull(request.getIntroduction()));
        user.setRoleId(roleId);
        updateById(user);
        operationLogRecordService.recordSuccess("USER", user.getId(), "UPDATE_USER", "编辑用户：" + user.getUserName());
        return toResponse(user);
    }

    @Override
    @Transactional
    public SysUserResponse updateUserStatus(Long id, SysUserStatusRequest request) {
        AdminAuthPrincipal current = AdminAuthContext.requireCurrentUser();
        SysUser user = requireExistingUser(id);
        String userStatus = normalizeStatus(requireText(request.getUserStatus(), "userStatus must not be blank"));
        SysRole role = requireRole(user.getRoleId());
        adminPermissionService.requireCanUpdateStatus(current, user, role);
        user.setUserStatus(userStatus);
        updateById(user);
        operationLogRecordService.recordSuccess("USER", user.getId(), "CHANGE_USER_STATUS", "禁用/启用用户：" + user.getUserName());
        return toResponse(user);
    }

    @Override
    @Transactional
    public SysUserResponse resetPassword(Long id, SysUserPasswordResetRequest request) {
        AdminAuthPrincipal current = AdminAuthContext.requireCurrentUser();
        SysUser user = requireExistingUser(id);
        String newPassword = requireText(request.getNewPassword(), "newPassword must not be blank");
        SysRole role = requireRole(user.getRoleId());
        adminPermissionService.requireCanResetPassword(current, user, role);
        user.setPasswordHash(passwordHashService.hash(newPassword));
        updateById(user);
        operationLogRecordService.recordSuccess("USER", user.getId(), "RESET_USER_PASSWORD", "重置用户密码：" + user.getUserName());
        return toResponse(user);
    }

    @Override
    @Transactional
    public SysUserResponse getCurrentProfile() {
        AdminAuthPrincipal current = AdminAuthContext.requireCurrentUser();
        return toResponse(requireExistingUser(current.getUserId()));
    }

    @Override
    @Transactional
    public SysUserResponse updateCurrentProfile(AdminProfileUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("profile request must not be null");
        }

        AdminAuthPrincipal current = AdminAuthContext.requireCurrentUser();
        SysUser user = requireExistingUser(current.getUserId());
        String email = trimToNull(request.getEmail());
        String phone = trimToNull(request.getPhone());
        assertUniqueEmailIfPresent(email, user.getId());
        assertUniquePhoneIfPresent(phone, user.getId());

        user.setNickName(trimToNull(request.getNickName()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        user.setIntroduction(trimToNull(request.getIntroduction()));
        updateById(user);
        operationLogRecordService.recordSuccess("USER_PROFILE", user.getId(), "UPDATE_PROFILE", "修改个人资料：" + user.getUserName());
        return toResponse(user);
    }

    @Override
    @Transactional
    public void changeCurrentPassword(AdminPasswordChangeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("password request must not be null");
        }

        AdminAuthPrincipal current = AdminAuthContext.requireCurrentUser();
        SysUser user = requireExistingUser(current.getUserId());
        String oldPassword = requireText(request.getOldPassword(), "oldPassword must not be blank");
        String newPassword = requireText(request.getNewPassword(), "newPassword must not be blank");
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("newPassword length must be at least 6");
        }
        if (!passwordHashService.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("原密码错误");
        }

        user.setPasswordHash(passwordHashService.hash(newPassword));
        updateById(user);
        operationLogRecordService.recordSuccess("USER_PROFILE", user.getId(), "CHANGE_OWN_PASSWORD", "修改自己密码：" + user.getUserName());
    }

    private SysUser requireExistingUser(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new IllegalArgumentException("user is not found");
        }
        return user;
    }

    private SysRole requireRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("roleId must not be blank");
        }
        SysRole role = sysRoleService.getById(roleId);
        if (role == null || !StringUtils.hasText(role.getRoleCode())) {
            throw new IllegalArgumentException("roleId is invalid");
        }
        return role;
    }

    private Long requireRoleId(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("roleId must not be blank");
        }
        return roleId;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeCreateStatus(String userStatus) {
        if (!StringUtils.hasText(userStatus)) {
            return AdminPermissionService.USER_STATUS_ENABLED;
        }
        return normalizeStatus(userStatus);
    }

    private String normalizeStatus(String userStatus) {
        String normalized = userStatus.trim().toUpperCase(Locale.ROOT);
        if (!AdminPermissionService.USER_STATUS_ENABLED.equals(normalized)
                && !AdminPermissionService.USER_STATUS_DISABLED.equals(normalized)) {
            throw new IllegalArgumentException("userStatus must be ENABLED or DISABLED");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void assertUniqueUserName(String userName, Long excludeId) {
        if (lambdaQuery().eq(SysUser::getUserName, userName)
                .ne(excludeId != null, SysUser::getId, excludeId)
                .count() > 0) {
            throw new IllegalArgumentException("userName already exists");
        }
    }

    private void assertUniqueEmail(String email, Long excludeId) {
        if (lambdaQuery().eq(SysUser::getEmail, email)
                .ne(excludeId != null, SysUser::getId, excludeId)
                .count() > 0) {
            throw new IllegalArgumentException("email already exists");
        }
    }

    private void assertUniquePhone(String phone, Long excludeId) {
        if (lambdaQuery().eq(SysUser::getPhone, phone)
                .ne(excludeId != null, SysUser::getId, excludeId)
                .count() > 0) {
            throw new IllegalArgumentException("phone already exists");
        }
    }

    private void assertUniqueEmailIfPresent(String email, Long excludeId) {
        if (StringUtils.hasText(email)) {
            assertUniqueEmail(email, excludeId);
        }
    }

    private void assertUniquePhoneIfPresent(String phone, Long excludeId) {
        if (StringUtils.hasText(phone)) {
            assertUniquePhone(phone, excludeId);
        }
    }

    private SysUserResponse toResponse(SysUser user) {
        return toResponse(user, requireRole(user.getRoleId()));
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
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setUserStatus(user.getUserStatus());
        response.setCreateTime(user.getCreateTime());
        response.setUpdateTime(user.getUpdateTime());
        return response;
    }

    private List<SysUserResponse> toResponseList(Collection<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        Map<Long, SysRole> rolesById = sysRoleService.listByIds(
                        users.stream()
                                .map(SysUser::getRoleId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(SysRole::getId, role -> role));
        return users.stream()
                .map(user -> toResponse(user, requireRole(rolesById.get(user.getRoleId()))))
                .toList();
    }

    private SysRole requireRole(SysRole role) {
        if (role == null || !StringUtils.hasText(role.getRoleCode())) {
            throw new IllegalArgumentException("roleId is invalid");
        }
        return role;
    }

    private List<Long> visibleRoleIds(AdminAuthPrincipal principal) {
        String roleCode = principal.getRoleCode();
        Set<Long> roleIds = new LinkedHashSet<>();
        for (SysRole role : sysRoleService.listRoles()) {
            if ("ADMIN".equalsIgnoreCase(roleCode) && "USER".equalsIgnoreCase(role.getRoleCode())) {
                roleIds.add(role.getId());
            } else if ("SUPER_ADMIN".equalsIgnoreCase(roleCode)
                    && ("ADMIN".equalsIgnoreCase(role.getRoleCode()) || "USER".equalsIgnoreCase(role.getRoleCode()))) {
                roleIds.add(role.getId());
            }
        }
        return List.copyOf(roleIds);
    }
}
