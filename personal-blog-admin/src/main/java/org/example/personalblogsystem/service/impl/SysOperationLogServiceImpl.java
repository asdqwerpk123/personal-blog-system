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

@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog>
        implements ISysOperationLogService {

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
