package org.example.personalblogsystem.service;

import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.entity.SysOperationLog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;

@Service
public class OperationLogRecordService {

    private static final int MAX_DETAIL_LENGTH = 255;

    private final ISysOperationLogService sysOperationLogService;

    public OperationLogRecordService(ISysOperationLogService sysOperationLogService) {
        this.sysOperationLogService = sysOperationLogService;
    }

    public void recordSuccess(String targetType, Long targetId, String actionType, String actionDetail) {
        AdminAuthPrincipal principal = AdminAuthContext.get();
        if (principal == null) {
            return;
        }
        record(principal.getUserId(), targetType, targetId, actionType, "SUCCESS", actionDetail);
    }

    public void recordSuccess(Long operatorUserId, String targetType, Long targetId, String actionType, String actionDetail) {
        record(operatorUserId, targetType, targetId, actionType, "SUCCESS", actionDetail);
    }

    public void recordFailure(Long operatorUserId, String targetType, Long targetId, String actionType, String actionDetail) {
        record(operatorUserId, targetType, targetId, actionType, "FAILURE", actionDetail);
    }

    public boolean replaceLatestSuccess(Long operatorUserId,
                                        String targetType,
                                        Long targetId,
                                        Collection<String> candidateActionTypes,
                                        String actionType,
                                        String actionDetail) {
        if (!StringUtils.hasText(targetType)
                || targetId == null
                || candidateActionTypes == null
                || candidateActionTypes.isEmpty()
                || !StringUtils.hasText(actionType)) {
            return false;
        }

        try {
            SysOperationLog operationLog = sysOperationLogService.lambdaQuery()
                    .eq(operatorUserId != null, SysOperationLog::getOperatorUserId, operatorUserId)
                    .isNull(operatorUserId == null, SysOperationLog::getOperatorUserId)
                    .eq(SysOperationLog::getTargetType, targetType.trim())
                    .eq(SysOperationLog::getTargetId, targetId)
                    .eq(SysOperationLog::getActionResult, "SUCCESS")
                    .in(SysOperationLog::getActionType, candidateActionTypes.stream()
                            .filter(StringUtils::hasText)
                            .map(String::trim)
                            .toList())
                    .orderByDesc(SysOperationLog::getId)
                    .last("limit 1")
                    .one();
            if (operationLog == null) {
                return false;
            }

            operationLog.setActionType(actionType.trim());
            operationLog.setActionDetail(normalizeDetail(actionDetail));
            operationLog.setUpdateTime(LocalDateTime.now());
            return sysOperationLogService.updateById(operationLog);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void record(Long operatorUserId,
                        String targetType,
                        Long targetId,
                        String actionType,
                        String actionResult,
                        String actionDetail) {
        if (!StringUtils.hasText(targetType)
                || !StringUtils.hasText(actionType)
                || !StringUtils.hasText(actionResult)) {
            return;
        }

        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setOperatorUserId(operatorUserId);
        operationLog.setTargetType(targetType.trim());
        operationLog.setTargetId(targetId);
        operationLog.setActionType(actionType.trim());
        operationLog.setActionResult(actionResult.trim());
        operationLog.setActionDetail(normalizeDetail(actionDetail));
        operationLog.setDeleted(false);

        try {
            sysOperationLogService.save(operationLog);
        } catch (RuntimeException ignored) {
            // Logging must not break the business operation it observes.
        }
    }

    private String normalizeDetail(String actionDetail) {
        String detail = StringUtils.hasText(actionDetail) ? actionDetail.trim() : "-";
        if (detail.length() <= MAX_DETAIL_LENGTH) {
            return detail;
        }
        return detail.substring(0, MAX_DETAIL_LENGTH);
    }
}
