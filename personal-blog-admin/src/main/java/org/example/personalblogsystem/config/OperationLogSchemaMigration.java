package org.example.personalblogsystem.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OperationLogSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public OperationLogSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
            // Older or restricted environments may not allow DDL here; logging remains best-effort.
        }
    }
}
