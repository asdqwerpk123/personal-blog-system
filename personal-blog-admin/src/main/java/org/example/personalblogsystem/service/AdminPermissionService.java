package org.example.personalblogsystem.service;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.entity.SysRole;
import org.example.personalblogsystem.entity.SysUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
public class AdminPermissionService {

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String USER_STATUS_ENABLED = "ENABLED";
    public static final String USER_STATUS_DISABLED = "DISABLED";
    private static final int FORBIDDEN_CODE = 403;
    private static final String FORBIDDEN_MESSAGE = "无权限管理该用户";

    public void requireUserManagementAccess(AdminAuthPrincipal principal) {
        String roleCode = requireRoleCode(principal);
        if (!ROLE_SUPER_ADMIN.equalsIgnoreCase(roleCode) && !ROLE_ADMIN.equalsIgnoreCase(roleCode)) {
            throw forbidden();
        }
    }

    public void requireCanCreateUser(AdminAuthPrincipal principal, SysRole targetRole) {
        requireUserManagementAccess(principal);
        requireRole(targetRole);
        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(targetRole.getRoleCode())) {
            throw forbidden();
        }
        requireCanAssignRequestedRole(principal, targetRole);
    }

    public void requireCanUpdateUser(AdminAuthPrincipal principal, SysUser targetUser, SysRole currentRole, SysRole requestedRole) {
        requireUserManagementAccess(principal);
        requireUser(targetUser);
        requireRole(currentRole);
        requireRole(requestedRole);
        requireCanManageCurrentRole(principal, targetUser, currentRole);
        boolean keepsOwnSuperAdminRole = isSelf(principal, targetUser)
                && Objects.equals(currentRole.getId(), requestedRole.getId())
                && ROLE_SUPER_ADMIN.equalsIgnoreCase(currentRole.getRoleCode());
        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(requestedRole.getRoleCode()) && !keepsOwnSuperAdminRole) {
            throw forbidden();
        }
        if (isSelf(principal, targetUser) && !Objects.equals(currentRole.getId(), requestedRole.getId())) {
            throw forbidden();
        }
        requireCanAssignRequestedRole(principal, requestedRole);
    }

    public void requireCanResetPassword(AdminAuthPrincipal principal, SysUser targetUser, SysRole currentRole) {
        requireUserManagementAccess(principal);
        requireUser(targetUser);
        requireRole(currentRole);
        if (isSelf(principal, targetUser)) {
            throw forbidden();
        }
        requireCanManageCurrentRole(principal, targetUser, currentRole);
    }

    public void requireCanUpdateStatus(AdminAuthPrincipal principal, SysUser targetUser, SysRole currentRole) {
        requireUserManagementAccess(principal);
        requireUser(targetUser);
        requireRole(currentRole);
        if (isSelf(principal, targetUser)) {
            throw forbidden();
        }
        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(currentRole.getRoleCode())) {
            throw forbidden();
        }
        requireCanManageCurrentRole(principal, targetUser, currentRole);
    }

    public void requireCanViewUser(AdminAuthPrincipal principal, SysUser targetUser, SysRole currentRole) {
        requireUserManagementAccess(principal);
        requireUser(targetUser);
        requireRole(currentRole);
        requireCanManageCurrentRole(principal, targetUser, currentRole);
    }

    public void requireValidStatus(String userStatus) {
        if (!StringUtils.hasText(userStatus)) {
            throw new IllegalArgumentException("userStatus must not be blank");
        }
        if (!USER_STATUS_ENABLED.equalsIgnoreCase(userStatus.trim())
                && !USER_STATUS_DISABLED.equalsIgnoreCase(userStatus.trim())) {
            throw new IllegalArgumentException("userStatus must be ENABLED or DISABLED");
        }
    }

    private void requireCanManageCurrentRole(AdminAuthPrincipal principal, SysUser targetUser, SysRole currentRole) {
        String operatorRoleCode = requireRoleCode(principal);
        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(operatorRoleCode)) {
            if (ROLE_SUPER_ADMIN.equalsIgnoreCase(currentRole.getRoleCode()) && !isSelf(principal, targetUser)) {
                throw forbidden();
            }
            return;
        }
        if (!ROLE_ADMIN.equalsIgnoreCase(operatorRoleCode)) {
            throw forbidden();
        }
        if (!ROLE_USER.equalsIgnoreCase(currentRole.getRoleCode())) {
            throw forbidden();
        }
    }

    private void requireCanAssignRequestedRole(AdminAuthPrincipal principal, SysRole requestedRole) {
        String operatorRoleCode = requireRoleCode(principal);
        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(operatorRoleCode)) {
            return;
        }
        if (!ROLE_ADMIN.equalsIgnoreCase(operatorRoleCode)) {
            throw forbidden();
        }
        if (!ROLE_USER.equalsIgnoreCase(requestedRole.getRoleCode())) {
            throw forbidden();
        }
    }

    private void requireRole(SysRole role) {
        if (role == null || !StringUtils.hasText(role.getRoleCode())) {
            throw new IllegalArgumentException("roleId is invalid");
        }
    }

    private void requireUser(SysUser user) {
        if (user == null) {
            throw new IllegalArgumentException("user is not found");
        }
    }

    private String requireRoleCode(AdminAuthPrincipal principal) {
        if (principal == null || !StringUtils.hasText(principal.getRoleCode())) {
            throw forbidden();
        }
        return principal.getRoleCode().trim();
    }

    private BlogException forbidden() {
        return new BlogException(FORBIDDEN_CODE, FORBIDDEN_MESSAGE);
    }

    private boolean isSelf(AdminAuthPrincipal principal, SysUser targetUser) {
        return principal != null && principal.getUserId() != null && principal.getUserId().equals(targetUser.getId());
    }
}
