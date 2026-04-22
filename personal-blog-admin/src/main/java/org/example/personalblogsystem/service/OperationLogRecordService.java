package org.example.personalblogsystem.service;

import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.entity.SysOperationLog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        record(operatorUserId, targetType, targetId, actionType, "FAILED", actionDetail);
    }

    private void record(Long operatorUserId,
                        String targetType,
                        Long targetId,
                        String actionType,
                        String actionResult,
                        String actionDetail) {
        if (operatorUserId == null || targetId == null
                || !StringUtils.hasText(targetType)
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
