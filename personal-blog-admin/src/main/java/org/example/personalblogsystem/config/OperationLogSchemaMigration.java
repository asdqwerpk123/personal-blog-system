package org.example.personalblogsystem.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 操作日志表结构兼容迁移器，应用启动时尝试修正历史 action_result 值和字段定义。
 * 依赖 JdbcTemplate 执行轻量 DDL/DML，适配课程设计环境中数据库结构可能不同步的情况。
 */
@Component
public class OperationLogSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public OperationLogSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 应用启动后执行操作日志表的兼容性修正。
     *
     * @param args Spring Boot 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.update("update sys_operation_log set action_result = 'FAILURE' where action_result = 'FAILED'");
            jdbcTemplate.execute("""
                    alter table sys_operation_log
                    modify operator_user_id bigint null comment '操作人用户ID',
                    modify target_id bigint null comment '目标对象ID',
                    modify action_result enum('SUCCESS', 'FAILURE') not null default 'SUCCESS' comment '操作结果'
                    """);
        } catch (RuntimeException ignored) {
            /**
             * 某些旧环境或受限数据库账号不允许启动时执行 DDL，迁移失败不影响业务继续运行。
             */
        }
    }
}
