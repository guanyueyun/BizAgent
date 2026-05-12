package com.example.bizagent.modules.system.controller;

import com.example.bizagent.common.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@RestController
@RequestMapping("/api/system")
public class DbInitController {
    private static final String ADMIN_PASSWORD_HASH = "$2a$10$p4K.xYwo/0/m6qQ7PVgbge33QWFRe0VMGExsCQjuE8TI1BwADbMxO";

    @Resource
    private DataSource dataSource;

    @GetMapping("/init-db")
    public ResponseEntity<String> initDb() {
        try {
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            
            String sql = """
                CREATE TABLE IF NOT EXISTS sys_user (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(255),
                    real_name VARCHAR(100),
                    email VARCHAR(100),
                    phone VARCHAR(20),
                    status INT DEFAULT 1,
                    create_by BIGINT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_by BIGINT,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    del_flag INT DEFAULT 0,
                    project_id BIGINT DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                
                CREATE TABLE IF NOT EXISTS sys_role (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    role_name VARCHAR(50) NOT NULL,
                    role_code VARCHAR(50) NOT NULL UNIQUE,
                    description VARCHAR(500),
                    status INT DEFAULT 1,
                    create_by BIGINT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_by BIGINT,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    del_flag INT DEFAULT 0,
                    project_id BIGINT DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                
                CREATE TABLE IF NOT EXISTS sys_menu (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    parent_id BIGINT DEFAULT 0,
                    menu_name VARCHAR(100) NOT NULL,
                    path VARCHAR(255),
                    component VARCHAR(255),
                    icon VARCHAR(100),
                    sort_order INT DEFAULT 0,
                    menu_type INT DEFAULT 1,
                    permission VARCHAR(100),
                    visible INT DEFAULT 1,
                    status INT DEFAULT 1,
                    create_by BIGINT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_by BIGINT,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    del_flag INT DEFAULT 0,
                    project_id BIGINT DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                
                CREATE TABLE IF NOT EXISTS sys_permission (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    permission_code VARCHAR(100) NOT NULL UNIQUE,
                    permission_name VARCHAR(100) NOT NULL,
                    module_name VARCHAR(100),
                    description VARCHAR(500),
                    create_by BIGINT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_by BIGINT,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    del_flag INT DEFAULT 0,
                    project_id BIGINT DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                
                CREATE TABLE IF NOT EXISTS sys_project (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    project_name VARCHAR(100) NOT NULL,
                    project_code VARCHAR(50) NOT NULL UNIQUE,
                    description VARCHAR(500),
                    status INT DEFAULT 1,
                    create_by BIGINT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_by BIGINT,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    del_flag INT DEFAULT 0,
                    project_id BIGINT DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                
                CREATE TABLE IF NOT EXISTS sys_module (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    module_name VARCHAR(100) NOT NULL,
                    module_code VARCHAR(50) NOT NULL UNIQUE,
                    description VARCHAR(500),
                    front_path VARCHAR(255),
                    back_path VARCHAR(255),
                    status INT DEFAULT 1,
                    lifecycle INT DEFAULT 0,
                    version VARCHAR(20) DEFAULT '1.0.0',
                    create_by BIGINT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_by BIGINT,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    del_flag INT DEFAULT 0,
                    project_id BIGINT DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                
                CREATE TABLE IF NOT EXISTS sys_role_menu (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    role_id BIGINT NOT NULL,
                    menu_id BIGINT NOT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                
                CREATE TABLE IF NOT EXISTS sys_role_permission (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    role_id BIGINT NOT NULL,
                    permission_id BIGINT NOT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                
                CREATE TABLE IF NOT EXISTS sys_user_role (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    role_id BIGINT NOT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                
                INSERT IGNORE INTO sys_project (id, project_name, project_code, description, status) 
                VALUES (1, '默认项目', 'default', '系统默认项目', 1);
                
                INSERT IGNORE INTO sys_role (id, role_name, role_code, description, status) 
                VALUES (1, '超级管理员', 'admin', '系统超级管理员', 1);
                
                INSERT IGNORE INTO sys_user (id, username, password, real_name, email, status, project_id) 
                VALUES (1, 'admin', '__ADMIN_PASSWORD_HASH__', '管理员', 'admin@example.com', 1, 1);
                
                INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);
                """.replace("__ADMIN_PASSWORD_HASH__", ADMIN_PASSWORD_HASH);
            
            for (String statementSql : sql.split(";")) {
                String trimmed = statementSql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            addColumnIfMissing(stmt, "sys_menu", "permission", "VARCHAR(100)");
            addColumnIfMissing(stmt, "sys_menu", "visible", "INT DEFAULT 1");
            addColumnIfMissing(stmt, "sys_module", "front_path", "VARCHAR(255)");
            addColumnIfMissing(stmt, "sys_module", "back_path", "VARCHAR(255)");
            addColumnIfMissing(stmt, "sys_module", "lifecycle", "INT DEFAULT 0");
            addColumnIfMissing(stmt, "sys_project", "project_id", "BIGINT DEFAULT 0");
            stmt.close();
            conn.close();
            
            return ResponseEntity.success("数据库初始化成功！");
        } catch (Exception e) {
            return ResponseEntity.error("初始化失败：" + e.getMessage());
        }
    }

    private void addColumnIfMissing(Statement stmt, String tableName, String columnName, String definition) {
        try {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        } catch (Exception ignored) {
        }
    }
}
