package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.dto.SysUserCreateRequest;
import org.example.personalblogsystem.dto.SysUserPasswordResetRequest;
import org.example.personalblogsystem.dto.SysUserResponse;
import org.example.personalblogsystem.dto.SysUserStatusRequest;
import org.example.personalblogsystem.dto.SysUserUpdateRequest;
import org.example.personalblogsystem.service.ISysUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/user")
public class SysUserController {

    private static final long MAX_PAGE_SIZE = 100L;

    private final ISysUserService sysUserService;

    public SysUserController(ISysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("/{id}")
    public Result<SysUserResponse> getById(@PathVariable Long id) {
        SysUserResponse user = sysUserService.getUserById(id);
        return user == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(user);
    }

    @GetMapping("/page")
    public Result<Page<SysUserResponse>> page(@RequestParam long current,
                                              @RequestParam long size,
                                              @RequestParam(required = false) String keyword) {
        validatePageRequest(current, size);
        return Result.ok(sysUserService.pageUsers(current, size, keyword));
    }

    @PostMapping
    public Result<SysUserResponse> create(@RequestBody SysUserCreateRequest request) {
        return Result.ok(sysUserService.createUser(request));
    }

    @PutMapping("/{id}")
    public Result<SysUserResponse> update(@PathVariable Long id, @RequestBody SysUserUpdateRequest request) {
        return Result.ok(sysUserService.updateUser(id, request));
    }

    @PutMapping("/{id}/status")
    public Result<SysUserResponse> updateStatus(@PathVariable Long id, @RequestBody SysUserStatusRequest request) {
        return Result.ok(sysUserService.updateUserStatus(id, request));
    }

    @PutMapping("/{id}/password/reset")
    public Result<SysUserResponse> resetPassword(@PathVariable Long id, @RequestBody SysUserPasswordResetRequest request) {
        return Result.ok(sysUserService.resetPassword(id, request));
    }

    private void validatePageRequest(long current, long size) {
        if (current <= 0) {
            throw new IllegalArgumentException("current must be greater than 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must not exceed " + MAX_PAGE_SIZE);
        }
    }
}
