package com.example.bizagent.config;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.sql.*;
import java.util.Properties;

@Configuration
public class MysqlInitializer {

    @PostConstruct
    public void init() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String host = env("DB_HOST", "localhost");
            String port = env("DB_PORT", "3306");
            Properties props = new Properties();
            props.setProperty("user", env("DB_USERNAME", "root"));
            props.setProperty("password", env("DB_PASSWORD", ""));
            props.setProperty("useSSL", "false");
            props.setProperty("serverTimezone", "Asia/Shanghai");
            props.setProperty("allowPublicKeyRetrieval", "true");
            props.setProperty("createDatabaseIfNotExist", "true");
            props.setProperty("useUnicode", "true");
            props.setProperty("characterEncoding", "utf8");
            props.setProperty("connectionCollation", "utf8mb4_unicode_ci");
            
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/", props
            );
            
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE IF NOT EXISTS bizagent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.execute("USE mysql");
            stmt.execute("GRANT ALL ON bizagent.* TO 'root'@'%'");
            stmt.execute("FLUSH PRIVILEGES");
            stmt.execute("USE bizagent");
            addColumnIfMissing(stmt, "sys_menu", "permission", "VARCHAR(100)");
            addColumnIfMissing(stmt, "sys_menu", "visible", "INT DEFAULT 1");
            addColumnIfMissing(stmt, "sys_module", "front_path", "VARCHAR(255)");
            addColumnIfMissing(stmt, "sys_module", "back_path", "VARCHAR(255)");
            addColumnIfMissing(stmt, "sys_module", "design_json", "LONGTEXT");
            addColumnIfMissing(stmt, "sys_module", "lifecycle", "INT DEFAULT 0");
            stmt.close();
            conn.close();
            System.out.println("[OK] MySQL database initialized successfully!");
        } catch (Exception e) {
            System.out.println("[WARN] MySQL init failed: " + e.getMessage());
        }
    }

    private void addColumnIfMissing(Statement stmt, String tableName, String columnName, String definition) {
        try {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        } catch (Exception ignored) {
        }
    }

    private String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
