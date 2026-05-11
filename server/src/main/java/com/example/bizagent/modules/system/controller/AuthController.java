package com.example.bizagent.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.common.auth.AuthTokenService;
import com.example.bizagent.common.auth.AuthUser;
import com.example.bizagent.common.auth.CurrentUser;
import com.example.bizagent.modules.system.entity.SysUser;
import com.example.bizagent.modules.system.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserService sysUserService;
    private final AuthTokenService authTokenService;
    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;

    public AuthController(SysUserService sysUserService, AuthTokenService authTokenService, 
                         DataSource dataSource, PasswordEncoder passwordEncoder) {
        this.sysUserService = sysUserService;
        this.authTokenService = authTokenService;
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        SysUser user = sysUserService.getOne(new QueryWrapper<SysUser>()
                .eq("username", request.username())
                .eq("del_flag", 0), false);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.error(401, "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            return ResponseEntity.error(403, "用户已禁用");
        }
        AuthUser authUser = new AuthUser(user.getId(), user.getUsername(), loadPermissions(user.getId()));
        Map<String, Object> result = new HashMap<>();
        result.put("token", authTokenService.createToken(authUser));
        result.put("user", user);
        result.put("permissions", authUser.permissions());
        return ResponseEntity.success("登录成功", result);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        Long userId = CurrentUser.id();
        SysUser user = sysUserService.getById(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("permissions", loadPermissions(userId));
        return ResponseEntity.success(result);
    }

    private List<String> loadPermissions(Long userId) {
        String sql = """
                SELECT DISTINCT p.permission_code
                FROM sys_user_role ur
                JOIN sys_role_permission rp ON rp.role_id = ur.role_id
                JOIN sys_permission p ON p.id = rp.permission_id
                WHERE ur.user_id = ? AND p.del_flag = 0
                """;
        List<String> permissions = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    permissions.add(resultSet.getString(1));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("加载用户权限失败", e);
        }
        return permissions;
    }

    public record LoginRequest(String username, String password) {
    }
}
