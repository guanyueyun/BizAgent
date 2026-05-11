
package com.example.bizagent.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.system.entity.SysUser;
import com.example.bizagent.modules.system.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/system/user")
public class SysUserController {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    public SysUserController(SysUserService sysUserService, PasswordEncoder passwordEncoder, DataSource dataSource) {
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('user:list')")
    public ResponseEntity<PageResponse<SysUser>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) String username,
                                                      @RequestParam(required = false) String realName) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        if (username != null && !username.isBlank()) {
            query.like("username", username.trim());
        }
        if (realName != null && !realName.isBlank()) {
            query.like("real_name", realName.trim());
        }
        IPage<SysUser> page = sysUserService.page(new Page<>(pageNum, pageSize), query);
        return ResponseEntity.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:list')")
    public ResponseEntity<SysUser> getById(@PathVariable Long id) {
        return ResponseEntity.success(sysUserService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:add')")
    public ResponseEntity<SysUser> create(@RequestBody SysUser user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        sysUserService.save(user);
        user.setPassword(null);
        return ResponseEntity.success("创建成功", user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<SysUser> update(@PathVariable Long id, @RequestBody SysUser user) {
        SysUser existingUser = sysUserService.getById(id);
        if (existingUser == null) {
            return ResponseEntity.error(404, "用户不存在");
        }
        user.setId(id);
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword(existingUser.getPassword());
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        sysUserService.updateById(user);
        user.setPassword(null);
        return ResponseEntity.success("更新成功", user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sysUserService.removeById(id);
        return ResponseEntity.success(null);
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:list')")
    public ResponseEntity<List<Long>> getUserRoles(@PathVariable Long id) {
        String sql = "SELECT role_id FROM sys_user_role WHERE user_id = ?";
        List<Long> roles = new java.util.ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            var resultSet = statement.executeQuery();
            while (resultSet.next()) {
                roles.add(resultSet.getLong(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("获取用户角色失败", e);
        }
        return ResponseEntity.success(roles);
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String deleteSql = "DELETE FROM sys_user_role WHERE user_id = ?";
                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                    deleteStmt.setLong(1, id);
                    deleteStmt.executeUpdate();
                }
                if (roleIds != null && !roleIds.isEmpty()) {
                    String insertSql = "INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        for (Long roleId : roleIds) {
                            insertStmt.setLong(1, id);
                            insertStmt.setLong(2, roleId);
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
            throw new IllegalStateException("分配角色失败", e);
        }
        return ResponseEntity.success(null);
    }
}
