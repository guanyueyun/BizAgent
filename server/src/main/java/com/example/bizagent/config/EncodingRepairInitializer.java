package com.example.bizagent.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

@Component
public class EncodingRepairInitializer {
    private static final Charset GBK = Charset.forName("GBK");
    private static final List<TableTextColumn> TEXT_COLUMNS = List.of(
            new TableTextColumn("sys_user", "real_name"),
            new TableTextColumn("sys_user", "phone"),
            new TableTextColumn("sys_role", "role_name"),
            new TableTextColumn("sys_role", "description"),
            new TableTextColumn("sys_menu", "menu_name"),
            new TableTextColumn("sys_permission", "permission_name"),
            new TableTextColumn("sys_permission", "module_name"),
            new TableTextColumn("sys_permission", "description"),
            new TableTextColumn("sys_project", "project_name"),
            new TableTextColumn("sys_project", "description"),
            new TableTextColumn("sys_module", "module_name"),
            new TableTextColumn("sys_module", "description"),
            new TableTextColumn("sys_model_config", "config_name"),
            new TableTextColumn("sys_model_config", "description")
    );

    private final DataSource dataSource;

    public EncodingRepairInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void repairMojibake() {
        try (Connection connection = dataSource.getConnection()) {
            int repaired = 0;
            for (TableTextColumn column : TEXT_COLUMNS) {
                if (hasColumn(connection, column.tableName(), column.columnName())) {
                    repaired += repairColumn(connection, column);
                }
            }
            repairDefaultSeedText(connection);
            if (repaired > 0) {
                System.out.println("Encoding repair completed, updated text fields: " + repaired);
            }
        } catch (Exception e) {
            System.out.println("Encoding repair skipped: " + e.getMessage());
        }
    }

    private int repairColumn(Connection connection, TableTextColumn column) throws Exception {
        String selectSql = "SELECT id, " + column.columnName() + " FROM " + column.tableName();
        String updateSql = "UPDATE " + column.tableName() + " SET " + column.columnName() + " = ? WHERE id = ?";
        int repaired = 0;
        try (Statement select = connection.createStatement();
             ResultSet resultSet = select.executeQuery(selectSql);
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                String value = resultSet.getString(column.columnName());
                String fixed = fixMojibake(value);
                if (fixed != null && !fixed.equals(value)) {
                    update.setString(1, fixed);
                    update.setLong(2, id);
                    update.addBatch();
                    repaired++;
                }
            }
            if (repaired > 0) {
                update.executeBatch();
            }
        }
        return repaired;
    }

    private String fixMojibake(String value) {
        if (value == null || value.isBlank() || !looksGarbled(value)) {
            return value;
        }
        String best = value;
        String gbkCandidate = decode(value, GBK);
        String latinCandidate = decode(value, StandardCharsets.ISO_8859_1);
        if (score(gbkCandidate) > score(best)) {
            best = gbkCandidate;
        }
        if (score(latinCandidate) > score(best)) {
            best = latinCandidate;
        }
        return best;
    }

    private String decode(String value, Charset sourceCharset) {
        try {
            return new String(value.getBytes(sourceCharset), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private int score(String value) {
        if (value == null) {
            return Integer.MIN_VALUE;
        }
        int score = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (isChinese(c)) {
                score += 3;
            }
            if (isGarbledMarker(c)) {
                score -= 8;
            }
            if (c == '\uFFFD' || Character.isISOControl(c)) {
                score -= 20;
            }
        }
        return score;
    }

    private boolean looksGarbled(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (isGarbledMarker(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isGarbledMarker(char c) {
        return c == '\uFFFD'
                || "锟脰脨娑鏃盲鈥âÃÐÑæçèéå绠悊鍛".indexOf(c) >= 0;
    }

    private boolean isChinese(char c) {
        return c >= '\u4E00' && c <= '\u9FFF';
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            if (columns.next()) {
                return true;
            }
        }
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }

    private void repairDefaultSeedText(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("UPDATE sys_user SET real_name = '管理员' WHERE username = 'admin' AND real_name <> '管理员'");
            statement.execute("UPDATE sys_role SET role_name = '超级管理员', description = '系统超级管理员' WHERE role_code = 'admin'");
            statement.execute("UPDATE sys_project SET project_name = '默认项目', description = '系统默认项目' WHERE project_code = 'default'");
        } catch (Exception ignored) {
        }
    }

    private record TableTextColumn(String tableName, String columnName) {
    }
}
