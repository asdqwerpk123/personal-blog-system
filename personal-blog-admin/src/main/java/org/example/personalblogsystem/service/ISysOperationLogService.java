package org.example.personalblogsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.personalblogsystem.entity.SysOperationLog;

public interface ISysOperationLogService extends IService<SysOperationLog> {

    Page<SysOperationLog> pageOperationLogs(long current, long size, Long operatorUserId, String targetType, String actionResult);
}
