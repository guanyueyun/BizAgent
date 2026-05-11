
package com.example.bizagent.modules.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.system.entity.SysRole;
import com.example.bizagent.modules.system.service.SysRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/system/role")
public class SysRoleController {

    private final SysRoleService sysRoleService;
    private final DataSource dataSource;

    public SysRoleController(SysRoleService sysRoleService, DataSource dataSource) {
        this.sysRoleService = sysRoleService;
        this.dataSource = dataSource;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('role:list')")
    public ResponseEntity<PageResponse<SysRole>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize) {
        IPage<SysRole> page = sysRoleService.page(new Page<>(pageNum, pageSize));
        return ResponseEntity.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:list')")
    public ResponseEntity<SysRole> getById(@PathVariable Long id) {
        return ResponseEntity.success(sysRoleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:add')")
    public ResponseEntity<SysRole> create(@RequestBody SysRole role) {
        sysRoleService.save(role);
        return ResponseEntity.success("创建成功", role);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:edit')")
    public ResponseEntity<SysRole> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        sysRoleService.updateById(role);
        return ResponseEntity.success("更新成功", role);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sysRoleService.removeById(id);
        return ResponseEntity.success(null);
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:list')")
    public ResponseEntity<List<Long>> getRolePermissions(@PathVariable Long id) {
        String sql = "SELECT permission_id FROM sys_role_permission WHERE role_id = ?";
        List<Long> permissions = new java.util.ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            var resultSet = statement.executeQuery();
            while (resultSet.next()) {
                permissions.add(resultSet.getLong(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("获取角色权限失败", e);
        }
        return ResponseEntity.success(permissions);
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:edit')")
    public ResponseEntity<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String deleteSql = "DELETE FROM sys_role_permission WHERE role_id = ?";
                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                    deleteStmt.setLong(1, id);
                    deleteStmt.executeUpdate();
                }
                if (permissionIds != null && !permissionIds.isEmpty()) {
                    String insertSql = "INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        for (Long permissionId : permissionIds) {
                            insertStmt.setLong(1, id);
                            insertStmt.setLong(2, permissionId);
                            insertStmt.addBatch();
                        }
                        insertStmt.executeBatch();
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("分配权限失败", e);
        }
        return ResponseEntity.success(null);
    }
}
