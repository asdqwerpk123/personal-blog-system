package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.entity.SysOperationLog;
import org.example.personalblogsystem.service.ISysOperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/operation-log")
public class SysOperationLogController {

    private static final long MAX_PAGE_SIZE = 100L;

    private final ISysOperationLogService sysOperationLogService;

    public SysOperationLogController(ISysOperationLogService sysOperationLogService) {
        this.sysOperationLogService = sysOperationLogService;
    }

    @GetMapping("/page")
    public Result<Page<SysOperationLog>> page(@RequestParam long current,
                                              @RequestParam long size,
                                              @RequestParam(required = false) Long operatorUserId,
                                              @RequestParam(required = false) String targetType,
                                              @RequestParam(required = false) String actionResult) {
        validatePageRequest(current, size);
        return Result.ok(sysOperationLogService.pageOperationLogs(current, size, operatorUserId, targetType, actionResult));
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
