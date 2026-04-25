package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.dto.AdminPasswordChangeRequest;
import org.example.personalblogsystem.dto.AdminProfileUpdateRequest;
import org.example.personalblogsystem.dto.SysUserResponse;
import org.example.personalblogsystem.service.ISysUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/profile")
public class AdminProfileController {

    private final ISysUserService sysUserService;

    public AdminProfileController(ISysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("/me")
    public Result<SysUserResponse> me() {
        return Result.ok(sysUserService.getCurrentProfile());
    }

    @PutMapping("/me")
    public Result<SysUserResponse> updateMe(@RequestBody AdminProfileUpdateRequest request) {
        return Result.ok(sysUserService.updateCurrentProfile(request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody AdminPasswordChangeRequest request) {
        sysUserService.changeCurrentPassword(request);
        return Result.ok(null);
    }
}
