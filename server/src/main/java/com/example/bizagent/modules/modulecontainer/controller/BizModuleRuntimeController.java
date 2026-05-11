package com.example.bizagent.modules.modulecontainer.controller;

import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.common.auth.CurrentUser;
import com.example.bizagent.modules.system.entity.SysModule;
import com.example.bizagent.modules.system.service.SysModuleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/biz/{moduleCode}")
public class BizModuleRuntimeController {

    private final DataSource dataSource;
    private final SysModuleService sysModuleService;

    public BizModuleRuntimeController(DataSource dataSource, SysModuleService sysModuleService) {
        this.dataSource = dataSource;
        this.sysModuleService = sysModuleService;
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@PathVariable("moduleCode") String moduleCode,
                                                    @RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) Long projectId) {
        String tableName = tableName(moduleCode);
        Long currentProjectId = currentProjectId(projectId);
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "list", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            long total = count(connection, tableName, currentProjectId);
            List<Map<String, Object>> records = queryForList(connection,
                    "SELECT * FROM " + tableName + " WHERE del_flag = 0 AND project_id = ? ORDER BY id DESC LIMIT ? OFFSET ?",
                    currentProjectId, pageSize, offset);
            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            return ResponseEntity.success(result);
        } catch (Exception e) {
            return ResponseEntity.error("查询模块数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable("moduleCode") String moduleCode,
                                                      @PathVariable("id") Long id,
                                                      @RequestParam(required = false) Long projectId) {
        String tableName = tableName(moduleCode);
        Long currentProjectId = currentProjectId(projectId);
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "list", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            List<Map<String, Object>> rows = queryForList(connection,
                    "SELECT * FROM " + tableName + " WHERE id = ? AND project_id = ? AND del_flag = 0", id, currentProjectId);
            return ResponseEntity.success(rows.isEmpty() ? null : rows.get(0));
        } catch (Exception e) {
            return ResponseEntity.error("查询模块详情失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@PathVariable("moduleCode") String moduleCode,
                                                      @RequestParam(required = false) Long projectId,
                                                      @RequestBody Map<String, Object> body) {
        String tableName = tableName(moduleCode);
        Long currentProjectId = currentProjectId(projectId);
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "add", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            Set<String> columns = writableColumns(connection, tableName);
            Map<String, Object> values = sanitizeBody(body, columns, currentProjectId);
            if (values.isEmpty()) {
                return ResponseEntity.error(400, "没有可写入字段");
            }
            String columnSql = String.join(", ", values.keySet());
            String placeholderSql = values.keySet().stream().map(key -> "?").collect(Collectors.joining(", "));
            String sql = "INSERT INTO " + tableName + " (" + columnSql + ") VALUES (" + placeholderSql + ")";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(statement, new ArrayList<>(values.values()));
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        body.put("id", keys.getLong(1));
                    }
                }
            }
            return ResponseEntity.success("创建成功", body);
        } catch (Exception e) {
            return ResponseEntity.error("创建模块数据失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable("moduleCode") String moduleCode,
                                                      @PathVariable("id") Long id,
                                                      @RequestParam(required = false) Long projectId,
                                                      @RequestBody Map<String, Object> body) {
        String tableName = tableName(moduleCode);
        Long currentProjectId = currentProjectId(projectId);
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "edit", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            Set<String> columns = writableColumns(connection, tableName);
            Map<String, Object> values = sanitizeBody(body, columns, currentProjectId);
            if (values.isEmpty()) {
                return ResponseEntity.error(400, "没有可更新字段");
            }
            String setSql = values.keySet().stream().map(key -> key + " = ?").collect(Collectors.joining(", "));
            List<Object> params = new ArrayList<>(values.values());
            params.add(id);
            params.add(currentProjectId);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE " + tableName + " SET " + setSql + " WHERE id = ? AND project_id = ?")) {
                bind(statement, params);
                statement.executeUpdate();
            }
            body.put("id", id);
            return ResponseEntity.success("更新成功", body);
        } catch (Exception e) {
            return ResponseEntity.error("更新模块数据失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("moduleCode") String moduleCode,
                                       @PathVariable("id") Long id,
                                       @RequestParam(required = false) Long projectId) {
        String tableName = tableName(moduleCode);
        Long currentProjectId = currentProjectId(projectId);
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "delete", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE " + tableName + " SET del_flag = 1 WHERE id = ? AND project_id = ?")) {
                statement.setLong(1, id);
                statement.setLong(2, currentProjectId);
                statement.executeUpdate();
            }
            return ResponseEntity.success("删除成功", null);
        } catch (Exception e) {
            return ResponseEntity.error("删除模块数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importData(@PathVariable("moduleCode") String moduleCode,
                                                          @RequestParam(required = false) Long projectId,
                                                          @RequestBody List<Map<String, Object>> rows) {
        String tableName = tableName(moduleCode);
        Long currentProjectId = currentProjectId(projectId);
        int successCount = 0;
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "import", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            Set<String> columns = writableColumns(connection, tableName);
            for (Map<String, Object> row : rows) {
                Map<String, Object> values = sanitizeBody(row, columns, currentProjectId);
                if (values.isEmpty()) {
                    continue;
                }
                String columnSql = String.join(", ", values.keySet());
                String placeholderSql = values.keySet().stream().map(key -> "?").collect(Collectors.joining(", "));
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO " + tableName + " (" + columnSql + ") VALUES (" + placeholderSql + ")")) {
                    bind(statement, new ArrayList<>(values.values()));
                    successCount += statement.executeUpdate();
                }
            }
            return ResponseEntity.success(Map.of("successCount", successCount, "totalCount", rows.size()));
        } catch (Exception e) {
            return ResponseEntity.error("导入模块数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportData(@PathVariable("moduleCode") String moduleCode,
                                             @RequestParam(required = false) Long projectId) {
        String tableName = tableName(moduleCode);
        Long currentProjectId = currentProjectId(projectId);
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "export", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            List<Map<String, Object>> rows = queryForList(connection,
                    "SELECT * FROM " + tableName + " WHERE del_flag = 0 AND project_id = ? ORDER BY id DESC", currentProjectId);
            return ResponseEntity.success(toCsv(rows));
        } catch (Exception e) {
            return ResponseEntity.error("导出模块数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics(@PathVariable("moduleCode") String moduleCode,
                                                          @RequestParam(required = false) Long projectId) {
        String tableName = tableName(moduleCode);
        Long currentProjectId = currentProjectId(projectId);
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "statistics", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("total", count(connection, tableName, currentProjectId));
            stats.put("draft", countByStatus(connection, tableName, "draft", currentProjectId));
            stats.put("submitted", countByStatus(connection, tableName, "submitted", currentProjectId));
            stats.put("approved", countByStatus(connection, tableName, "approved", currentProjectId));
            stats.put("rejected", countByStatus(connection, tableName, "rejected", currentProjectId));
            return ResponseEntity.success(stats);
        } catch (Exception e) {
            return ResponseEntity.error("查询统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Map<String, Object>>> notifications(@PathVariable("moduleCode") String moduleCode,
                                                                   @RequestParam(required = false) Long projectId) {
        String tableName = notificationTableName(moduleCode);
        Long currentProjectId = currentProjectId(projectId);
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "notify", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            return ResponseEntity.success(queryForList(connection,
                    "SELECT * FROM " + tableName + " WHERE del_flag = 0 AND project_id = ? ORDER BY id DESC", currentProjectId));
        } catch (Exception e) {
            return ResponseEntity.error("查询消息提醒失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/notify")
    public ResponseEntity<Void> notify(@PathVariable("moduleCode") String moduleCode,
                                       @PathVariable("id") Long id,
                                       @RequestParam(required = false) Long projectId,
                                       @RequestBody Map<String, Object> body) {
        String tableName = notificationTableName(moduleCode);
        String title = Objects.toString(body.getOrDefault("title", "业务提醒"));
        String content = Objects.toString(body.getOrDefault("content", "请及时处理业务数据 #" + id));
        String receiver = Objects.toString(body.getOrDefault("receiver", "admin"));
        Long currentProjectId = currentProjectId(projectId);
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "notify", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO " + tableName + " (biz_id, receiver, message_title, message_content, read_status, project_id) VALUES (?, ?, ?, ?, 'unread', ?)")) {
                statement.setLong(1, id);
                statement.setString(2, receiver);
                statement.setString(3, title);
                statement.setString(4, content);
                statement.setLong(5, currentProjectId);
                statement.executeUpdate();
            }
            return ResponseEntity.success("提醒发送成功", null);
        } catch (Exception e) {
            return ResponseEntity.error("发送消息提醒失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submit(@PathVariable("moduleCode") String moduleCode,
                                       @PathVariable("id") Long id,
                                       @RequestParam(required = false) Long projectId) {
        String tableName = tableName(moduleCode);
        Long currentProjectId = currentProjectId(projectId);
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "edit", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            updateStatus(connection, tableName, id, "submitted", currentProjectId);
            return ResponseEntity.success("提交成功", null);
        } catch (Exception e) {
            return ResponseEntity.error("提交审批失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable("moduleCode") String moduleCode,
                                        @PathVariable("id") Long id,
                                        @RequestParam(required = false) Long projectId,
                                        @RequestBody Map<String, Object> body) {
        String tableName = tableName(moduleCode);
        String approvalTable = "biz_" + safeModuleCode(moduleCode) + "_approval";
        String result = Objects.toString(body.getOrDefault("result", "approved"));
        String comment = Objects.toString(body.getOrDefault("comment", ""));
        Long currentProjectId = currentProjectId(projectId);
        try (Connection connection = dataSource.getConnection()) {
            ensureModuleAvailable(moduleCode, currentProjectId);
            enforcePermission(connection, moduleCode, "approve", CurrentUser.id(), currentProjectId);
            ensureTableExists(connection, tableName);
            ensureTableExists(connection, approvalTable);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO " + approvalTable + " (biz_id, node_name, approver, approval_status, approval_comment, approval_time, project_id) VALUES (?, ?, ?, ?, ?, NOW(), ?)")) {
                statement.setLong(1, id);
                statement.setString(2, "默认审批节点");
                statement.setString(3, "admin");
                statement.setString(4, result);
                statement.setString(5, comment);
                statement.setLong(6, currentProjectId);
                statement.executeUpdate();
            }
            updateStatus(connection, tableName, id, result, currentProjectId);
            return ResponseEntity.success("审批完成", null);
        } catch (Exception e) {
            return ResponseEntity.error("审批处理失败: " + e.getMessage());
        }
    }

    private String tableName(String moduleCode) {
        String safeCode = safeModuleCode(moduleCode);
        return "biz_" + safeCode + "_main";
    }

    private String notificationTableName(String moduleCode) {
        return "biz_" + safeModuleCode(moduleCode) + "_notification";
    }

    private String safeModuleCode(String moduleCode) {
        String safeCode = moduleCode.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (!safeCode.matches("[a-z][a-z0-9_]{1,40}")) {
            throw new IllegalArgumentException("非法模块编码");
        }
        return safeCode;
    }

    private Long currentProjectId(Long projectId) {
        return projectId == null || projectId <= 0 ? 1L : projectId;
    }

    private void ensureModuleAvailable(String moduleCode, Long projectId) {
        SysModule module = sysModuleService.getOne(new QueryWrapper<SysModule>()
                .eq("module_code", safeModuleCode(moduleCode))
                .eq("project_id", projectId)
                .eq("del_flag", 0), false);
        if (module == null) {
            throw new IllegalArgumentException("当前项目下模块不存在");
        }
        if (!Integer.valueOf(1).equals(module.getStatus()) || !Integer.valueOf(2).equals(module.getLifecycle())) {
            throw new IllegalStateException("模块未部署或未加载");
        }
    }

    private void ensureTableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            if (!tables.next()) {
                throw new IllegalArgumentException("业务表不存在: " + tableName);
            }
        }
    }

    private long count(Connection connection, String tableName, Long projectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName + " WHERE del_flag = 0 AND project_id = ?")) {
            statement.setLong(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    private long countByStatus(Connection connection, String tableName, String status, Long projectId) throws SQLException {
        if (!hasColumn(connection, tableName, "status")) {
            return 0L;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + tableName + " WHERE del_flag = 0 AND status = ? AND project_id = ?")) {
            statement.setString(1, status);
            statement.setLong(2, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    private List<Map<String, Object>> queryForList(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData metaData = resultSet.getMetaData();
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    private Set<String> writableColumns(Connection connection, String tableName) throws SQLException {
        Set<String> excluded = Set.of("id", "create_time", "update_time", "del_flag", "project_id");
        Set<String> columns = new LinkedHashSet<>();
        try (ResultSet resultSet = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, null)) {
            while (resultSet.next()) {
                String column = resultSet.getString("COLUMN_NAME");
                if (!excluded.contains(column)) {
                    columns.add(column);
                }
            }
        }
        return columns;
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private Map<String, Object> sanitizeBody(Map<String, Object> body, Set<String> writableColumns, Long projectId) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            if (writableColumns.contains(entry.getKey())) {
                values.put(entry.getKey(), entry.getValue());
            }
        }
        if (!values.isEmpty()) {
            values.put("project_id", projectId);
        }
        return values;
    }

    private void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }

    private void updateStatus(Connection connection, String tableName, Long id, String status, Long projectId) throws SQLException {
        if (!hasColumn(connection, tableName, "status")) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("UPDATE " + tableName + " SET status = ? WHERE id = ? AND project_id = ?")) {
            statement.setString(1, status);
            statement.setLong(2, id);
            statement.setLong(3, projectId);
            statement.executeUpdate();
        }
    }

    private void enforcePermission(Connection connection, String moduleCode, String action, Long userId, Long projectId) throws SQLException {
        if (Long.valueOf(1L).equals(userId)) {
            return;
        }
        String permissionCode = safeModuleCode(moduleCode) + ":" + action;
        try (PreparedStatement exists = connection.prepareStatement("SELECT COUNT(*) FROM sys_permission WHERE permission_code = ? AND project_id = ? AND del_flag = 0")) {
            exists.setString(1, permissionCode);
            exists.setLong(2, projectId);
            try (ResultSet resultSet = exists.executeQuery()) {
                if (resultSet.next() && resultSet.getLong(1) == 0L) {
                    throw new SecurityException("模块未开放操作权限: " + permissionCode);
                }
            }
        }
        String sql = """
                SELECT COUNT(*)
                FROM sys_user_role ur
                JOIN sys_role_permission rp ON rp.role_id = ur.role_id
                JOIN sys_permission p ON p.id = rp.permission_id
                WHERE ur.user_id = ? AND p.permission_code = ? AND p.project_id = ? AND p.del_flag = 0
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, permissionCode);
            statement.setLong(3, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getLong(1) > 0) {
                    return;
                }
            }
        }
        throw new SecurityException("缺少权限: " + permissionCode);
    }

    private String toCsv(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "";
        }
        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        StringBuilder csv = new StringBuilder(String.join(",", headers)).append("\n");
        for (Map<String, Object> row : rows) {
            csv.append(headers.stream()
                    .map(header -> csvCell(row.get(header)))
                    .collect(Collectors.joining(","))).append("\n");
        }
        return csv.toString();
    }

    private String csvCell(Object value) {
        String text = Objects.toString(value, "");
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
