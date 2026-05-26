package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogsystem.entity.SysOperationLog;
import org.example.personalblogsystem.mapper.SysOperationLogMapper;
import org.example.personalblogsystem.service.ISysOperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 操作日志服务实现类，基于 MyBatis Plus 构造动态查询条件并返回分页结果。
 * 主要支撑后台审计日志列表的筛选、排序和历史结果值兼容。
 */
@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog>
        implements ISysOperationLogService {

    /**
     * 按条件分页查询操作日志。
     *
     * @param current 当前页码，从 1 开始
     * @param size 每页条数
     * @param operatorUserId 操作人用户主键，可为空
     * @param targetType 操作对象类型，可为空
     * @param actionResult 操作结果，可为空；支持 SUCCESS、FAILURE
     * @return 按创建时间和主键倒序排列的日志分页结果
     * @throws IllegalArgumentException actionResult 非法时抛出
     */
    @Override
    public Page<SysOperationLog> pageOperationLogs(long current,
                                                    long size,
                                                    Long operatorUserId,
                                                    String targetType,
                                                    String actionResult) {
        LambdaQueryWrapper<SysOperationLog> queryWrapper = new LambdaQueryWrapper<>();
        if (operatorUserId != null) {
            queryWrapper.eq(SysOperationLog::getOperatorUserId, operatorUserId);
        }
        if (StringUtils.hasText(targetType)) {
            queryWrapper.eq(SysOperationLog::getTargetType, targetType.trim());
        }
        if (actionResult != null) {
            String normalizedActionResult = normalizeActionResult(actionResult);
            if ("FAILURE".equals(normalizedActionResult)) {
                queryWrapper.in(SysOperationLog::getActionResult, "FAILURE", "FAILED");
            } else {
                queryWrapper.eq(SysOperationLog::getActionResult, normalizedActionResult);
            }
        }
        queryWrapper.orderByDesc(SysOperationLog::getCreateTime, SysOperationLog::getId);
        return page(new Page<>(current, size), queryWrapper);
    }

    private String normalizeActionResult(String actionResult) {
        String value = actionResult == null ? null : actionResult.trim();
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("actionResult must be one of SUCCESS, FAILURE");
        }

        String normalized = value.toUpperCase(Locale.ROOT);
        if ("FAILED".equals(normalized)) {
            return "FAILURE";
        }
        if (!"SUCCESS".equals(normalized) && !"FAILURE".equals(normalized)) {
            throw new IllegalArgumentException("actionResult must be one of SUCCESS, FAILURE");
        }
        return normalized;
    }
}
