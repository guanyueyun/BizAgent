package com.example.bizagent.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Configuration
public class DatabaseInitializer {
    private static final String ADMIN_PASSWORD_HASH = "$2a$10$p4K.xYwo/0/m6qQ7PVgbge33QWFRe0VMGExsCQjuE8TI1BwADbMxO";

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.url}")
    private String url;

    @PostConstruct
    public void init() {
        if (!url.startsWith("jdbc:mysql:")) {
            migrateDefaultAdminPassword();
            return;
        }
        try {
            String urlWithoutParams = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
            String baseUrl = urlWithoutParams.substring(0, urlWithoutParams.lastIndexOf('/'));
            Connection conn = DriverManager.getConnection(baseUrl, username, password);
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE IF NOT EXISTS bizagent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.execute("GRANT ALL PRIVILEGES ON bizagent.* TO '" + username + "'@'%'");
            stmt.execute("FLUSH PRIVILEGES");
            stmt.close();
            conn.close();
            System.out.println("[OK] Database initialized successfully!");
        } catch (Exception e) {
            System.out.println("[WARN] Database init failed: " + e.getMessage());
        }
        migratePlatformColumns();
        migrateDefaultAdminPassword();
    }

    private void migratePlatformColumns() {
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS sys_model_config (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                        config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
                        provider VARCHAR(50) NOT NULL COMMENT '模型厂商',
                        base_url VARCHAR(500) COMMENT '接口地址',
                        api_key VARCHAR(500) COMMENT 'API Key',
                        model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
                        temperature DECIMAL(4,2) DEFAULT 0.70 COMMENT '温度',
                        max_tokens INT DEFAULT 4096 COMMENT '最大Token数',
                        timeout_seconds INT DEFAULT 60 COMMENT '超时时间秒',
                        retry_count INT DEFAULT 0 COMMENT '失败重试次数',
                        default_flag INT DEFAULT 0 COMMENT '是否默认 0否 1是',
                        status INT DEFAULT 1 COMMENT '状态 0禁用 1启用',
                        description VARCHAR(500) COMMENT '描述',
                        create_by BIGINT COMMENT '创建人',
                        create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        update_by BIGINT COMMENT '更新人',
                        update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        del_flag INT DEFAULT 0 COMMENT '删除标志 0正常 1删除',
                        project_id BIGINT DEFAULT 0 COMMENT '项目ID'
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型配置表'
                    """);
            addColumnIfMissing(stmt, "sys_menu", "permission", "VARCHAR(100)");
            addColumnIfMissing(stmt, "sys_menu", "visible", "INT DEFAULT 1");
            addColumnIfMissing(stmt, "sys_module", "front_path", "VARCHAR(255)");
            addColumnIfMissing(stmt, "sys_module", "back_path", "VARCHAR(255)");
            addColumnIfMissing(stmt, "sys_module", "design_json", "LONGTEXT");
            addColumnIfMissing(stmt, "sys_module", "lifecycle", "INT DEFAULT 0");
            addColumnIfMissing(stmt, "sys_model_config", "retry_count", "INT DEFAULT 0");
            addColumnIfMissing(stmt, "sys_project", "project_id", "BIGINT DEFAULT 0");
            stmt.execute("""
                    INSERT INTO sys_model_config
                    (id, config_name, provider, base_url, model_name, temperature, max_tokens, timeout_seconds, default_flag, status, description, project_id)
                    SELECT 1, '默认模型', 'OpenAI-Compatible', 'https://api.openai.com/v1', 'gpt-4o-mini', 0.70, 4096, 60, 1, 1, '默认兼容 OpenAI Chat Completions 的模型配置', 1
                    WHERE NOT EXISTS (SELECT 1 FROM sys_model_config WHERE id = 1)
                    """);
            seedPermission(stmt, 26, "model-config:list", "模型配置查看", "查看模型配置");
            seedPermission(stmt, 27, "model-config:add", "模型配置新增", "新增模型配置");
            seedPermission(stmt, 28, "model-config:edit", "模型配置编辑", "编辑模型配置");
            seedPermission(stmt, 29, "model-config:delete", "模型配置删除", "删除模型配置");
            seedPermission(stmt, 30, "ai:generate", "AI生成", "分析、优化和生成模块草案");
            seedPermission(stmt, 31, "ai:preview", "AI预览", "预览AI生成模块");
            seedPermission(stmt, 32, "ai:publish", "AI发布", "保存、发布和部署AI生成模块");
            seedRolePermission(stmt, 26);
            seedRolePermission(stmt, 27);
            seedRolePermission(stmt, 28);
            seedRolePermission(stmt, 29);
            seedRolePermission(stmt, 30);
            seedRolePermission(stmt, 31);
            seedRolePermission(stmt, 32);
        } catch (Exception e) {
            System.out.println("[WARN] Platform schema migration skipped: " + e.getMessage());
        }
    }

    private void seedPermission(Statement stmt, int id, String code, String name, String description) throws Exception {
        stmt.execute("INSERT INTO sys_permission (id, permission_code, permission_name, module_name, description) " +
                "SELECT " + id + ", '" + code + "', '" + name + "', '系统管理', '" + description + "' " +
                "WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = '" + code + "')");
    }

    private void seedRolePermission(Statement stmt, int permissionId) throws Exception {
        stmt.execute("INSERT INTO sys_role_permission (role_id, permission_id) " +
                "SELECT 1, " + permissionId + " WHERE NOT EXISTS " +
                "(SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = " + permissionId + ")");
    }

    private void addColumnIfMissing(Statement stmt, String tableName, String columnName, String definition) {
        try {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        } catch (Exception ignored) {
        }
    }

    private void migrateDefaultAdminPassword() {
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE sys_user SET password = '" + ADMIN_PASSWORD_HASH + "' " +
                    "WHERE username = 'admin' AND (password = '123456' OR password IS NULL OR password NOT LIKE '$2%')");
        } catch (Exception e) {
            System.out.println("Default admin password migration skipped: " + e.getMessage());
        }
    }
}
