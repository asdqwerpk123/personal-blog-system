package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogsystem.entity.SysUser;
import org.example.personalblogsystem.service.ISysUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        return user == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(user);
    }

    @GetMapping("/page")
    public Result<Page<SysUser>> page(@RequestParam long current,
                                      @RequestParam long size,
                                      @RequestParam(required = false) String keyword) {
        validatePageRequest(current, size);
        return Result.ok(sysUserService.pageUsers(current, size, keyword));
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
