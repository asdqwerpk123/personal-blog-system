package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.entity.SysOperationLog;
import org.example.personalblogsystem.service.ISysOperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台操作日志查询控制器，提供管理端分页检索审计日志的接口。
 * 依赖 ISysOperationLogService 组合过滤条件，支持按操作人、对象类型和操作结果筛选。
 */
@RestController
@RequestMapping({"/admin/operation-log", "/admin/log"})
public class SysOperationLogController {

    /**
     * 操作日志分页查询最大页大小，防止一次请求拉取过多审计数据。
     */
    private static final long MAX_PAGE_SIZE = 100L;

    private final ISysOperationLogService sysOperationLogService;

    public SysOperationLogController(ISysOperationLogService sysOperationLogService) {
        this.sysOperationLogService = sysOperationLogService;
    }

    /**
     * 分页查询操作日志。
     *
     * @param current 当前页码，从 1 开始
     * @param size 每页条数，不能超过 MAX_PAGE_SIZE
     * @param operatorUserId 操作人用户主键，可为空
     * @param targetType 操作对象类型，可为空
     * @param actionResult 操作结果，可为空；支持 SUCCESS、FAILURE
     * @return 符合条件的操作日志分页结果
     * @throws IllegalArgumentException 分页参数或操作结果参数不合法时抛出
     */
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
