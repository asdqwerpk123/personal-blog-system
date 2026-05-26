package org.example.personalblogsystem.service;

import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.entity.SysOperationLog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 操作日志记录服务，集中封装后台业务操作的成功、失败和日志修正逻辑。
 * 当前项目未使用独立 AOP 切面，而是在核心业务服务中显式调用该服务写入 sys_operation_log。
 */
@Service
public class OperationLogRecordService {

    /**
     * 操作详情字段最大长度，避免超出数据库字段限制。
     */
    private static final int MAX_DETAIL_LENGTH = 255;

    private final ISysOperationLogService sysOperationLogService;

    public OperationLogRecordService(ISysOperationLogService sysOperationLogService) {
        this.sysOperationLogService = sysOperationLogService;
    }

    /**
     * 记录当前管理员上下文中的成功操作。
     *
     * @param targetType 操作对象类型，如 ARTICLE、CATEGORY、AUTH
     * @param targetId 操作对象主键，可为空
     * @param actionType 操作类型，如 CREATE、UPDATE、LOGIN_SUCCESS
     * @param actionDetail 操作详情，超长时会被截断
     */
    public void recordSuccess(String targetType, Long targetId, String actionType, String actionDetail) {
        AdminAuthPrincipal principal = AdminAuthContext.get();
        if (principal == null) {
            return;
        }
        record(principal.getUserId(), targetType, targetId, actionType, "SUCCESS", actionDetail);
    }

    /**
     * 按指定操作人记录成功操作。
     *
     * @param operatorUserId 操作人用户主键，可为空表示未知操作人
     * @param targetType 操作对象类型
     * @param targetId 操作对象主键，可为空
     * @param actionType 操作类型
     * @param actionDetail 操作详情，超长时会被截断
     */
    public void recordSuccess(Long operatorUserId, String targetType, Long targetId, String actionType, String actionDetail) {
        record(operatorUserId, targetType, targetId, actionType, "SUCCESS", actionDetail);
    }

    /**
     * 按指定操作人记录失败操作。
     *
     * @param operatorUserId 操作人用户主键，可为空表示未知操作人
     * @param targetType 操作对象类型
     * @param targetId 操作对象主键，可为空
     * @param actionType 操作类型
     * @param actionDetail 失败详情，超长时会被截断
     */
    public void recordFailure(Long operatorUserId, String targetType, Long targetId, String actionType, String actionDetail) {
        record(operatorUserId, targetType, targetId, actionType, "FAILURE", actionDetail);
    }

    /**
     * 将最近一条匹配的成功日志替换为新的操作类型和详情。
     *
     * @param operatorUserId 操作人用户主键，可为空表示匹配未知操作人
     * @param targetType 操作对象类型
     * @param targetId 操作对象主键
     * @param candidateActionTypes 候选旧操作类型集合
     * @param actionType 新操作类型
     * @param actionDetail 新操作详情
     * @return 找到并更新日志返回 true，否则返回 false
     */
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
            /**
             * 只按最近一条成功日志修正，避免重复写入相近操作时覆盖更早的审计记录。
             */
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
            /**
             * 日志失败不能影响主业务提交，因此吞掉记录异常。
             */
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
