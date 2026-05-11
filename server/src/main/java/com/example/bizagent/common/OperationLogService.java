package com.example.bizagent.common;

import com.example.bizagent.common.auth.CurrentUser;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

@Service
public class OperationLogService {

    private final DataSource dataSource;

    public OperationLogService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void log(String operationType, String moduleCode, Long projectId, String status, String message) {
        try (Connection connection = dataSource.getConnection()) {
            ensureTable(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO sys_operation_log
                    (operation_type, module_code, user_id, project_id, status, message)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, operationType);
                statement.setString(2, moduleCode);
                statement.setLong(3, safeUserId());
                statement.setLong(4, projectId == null ? 1L : projectId);
                statement.setString(5, status);
                statement.setString(6, message == null ? "" : message.substring(0, Math.min(message.length(), 1000)));
                statement.executeUpdate();
            }
        } catch (Exception ignored) {
        }
    }

    private long safeUserId() {
        try {
            return CurrentUser.id();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private void ensureTable(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS sys_operation_log (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                        operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
                        module_code VARCHAR(100) COMMENT '模块编码',
                        user_id BIGINT COMMENT '操作用户',
                        project_id BIGINT DEFAULT 1 COMMENT '项目ID',
                        status VARCHAR(20) NOT NULL COMMENT '状态',
                        message VARCHAR(1000) COMMENT '日志内容',
                        create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表'
                    """);
        }
    }
}
