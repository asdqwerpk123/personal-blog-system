package org.example.personalblogsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.personalblogsystem.entity.SysOperationLog;

/**
 * 操作日志服务接口，扩展 MyBatis Plus 通用 CRUD 并定义后台分页查询能力。
 */
public interface ISysOperationLogService extends IService<SysOperationLog> {

    /**
     * 按筛选条件分页查询操作日志。
     *
     * @param current 当前页码，从 1 开始
     * @param size 每页条数
     * @param operatorUserId 操作人用户主键，可为空
     * @param targetType 操作对象类型，可为空
     * @param actionResult 操作结果，可为空；支持 SUCCESS、FAILURE
     * @return 操作日志分页结果
     */
    Page<SysOperationLog> pageOperationLogs(long current, long size, Long operatorUserId, String targetType, String actionResult);
}
