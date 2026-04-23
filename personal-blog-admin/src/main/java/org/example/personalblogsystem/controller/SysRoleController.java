package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.entity.SysRole;
import org.example.personalblogsystem.service.AdminPermissionService;
import org.example.personalblogsystem.service.ISysRoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/role")
public class SysRoleController {

    private final ISysRoleService sysRoleService;
    private final AdminPermissionService adminPermissionService;

    public SysRoleController(ISysRoleService sysRoleService, AdminPermissionService adminPermissionService) {
        this.sysRoleService = sysRoleService;
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping("/{id}")
    public Result<SysRole> getById(@PathVariable Long id) {
        adminPermissionService.requireUserManagementAccess(AdminAuthContext.requireCurrentUser());
        SysRole role = sysRoleService.getById(id);
        return role == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(role);
    }

    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        var currentUser = AdminAuthContext.requireCurrentUser();
        adminPermissionService.requireUserManagementAccess(currentUser);
        return Result.ok(filterAssignableRoles(currentUser.getRoleCode(), sysRoleService.listRoles()));
    }

    private List<SysRole> filterAssignableRoles(String roleCode, List<SysRole> roles) {
        return roles.stream()
                .filter(role -> {
                    if ("ADMIN".equalsIgnoreCase(roleCode)) {
                        return "USER".equalsIgnoreCase(role.getRoleCode());
                    }
                    return !"SUPER_ADMIN".equalsIgnoreCase(role.getRoleCode());
                })
                .toList();
    }
}
