package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.auth.UserAuthContext;
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
@RequestMapping("/user/profile")
public class UserProfileController {

    private final ISysUserService sysUserService;

    public UserProfileController(ISysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("/me")
    public Result<SysUserResponse> me() {
        Long userId = UserAuthContext.requireCurrentUser().getUserId();
        return Result.ok(sysUserService.getUserProfile(userId));
    }

    @PutMapping("/me")
    public Result<SysUserResponse> updateMe(@RequestBody AdminProfileUpdateRequest request) {
        Long userId = UserAuthContext.requireCurrentUser().getUserId();
        return Result.ok(sysUserService.updateUserProfile(userId, request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody AdminPasswordChangeRequest request) {
        Long userId = UserAuthContext.requireCurrentUser().getUserId();
        sysUserService.changeUserPassword(userId, request);
        return Result.ok(null);
    }
}
