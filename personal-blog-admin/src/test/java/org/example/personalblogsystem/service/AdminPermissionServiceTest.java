package org.example.personalblogsystem.service;

import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.entity.SysRole;
import org.example.personalblogsystem.entity.SysUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminPermissionServiceTest {

    private final AdminPermissionService adminPermissionService = new AdminPermissionService();

    @Test
    void shouldRejectUnknownRoleForManagementAccess() {
        assertThatThrownBy(() ->
                adminPermissionService.requireUserManagementAccess(new AdminAuthPrincipal(7L, "ghost", 99L, "GUEST")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("无权限管理该用户");
    }

    @Test
    void shouldAllowSuperAdminToUpdateOwnProfileWhenRoleIsUnchanged() {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(1L, "root", 1L, "SUPER_ADMIN");
        SysUser targetUser = new SysUser();
        targetUser.setId(1L);
        targetUser.setRoleId(1L);
        SysRole superAdminRole = new SysRole();
        superAdminRole.setId(1L);
        superAdminRole.setRoleCode("SUPER_ADMIN");

        assertThatCode(() ->
                adminPermissionService.requireCanUpdateUser(principal, targetUser, superAdminRole, superAdminRole))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectResettingCurrentUserPasswordInUserManagement() {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(2L, "admin", 2L, "ADMIN");
        SysUser targetUser = new SysUser();
        targetUser.setId(2L);
        targetUser.setRoleId(2L);
        SysRole adminRole = new SysRole();
        adminRole.setId(2L);
        adminRole.setRoleCode("ADMIN");

        assertThatThrownBy(() ->
                adminPermissionService.requireCanResetPassword(principal, targetUser, adminRole))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("无权限管理该用户");
    }
}
