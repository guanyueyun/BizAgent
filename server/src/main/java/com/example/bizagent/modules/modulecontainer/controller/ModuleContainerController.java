
package com.example.bizagent.modules.modulecontainer.controller;

import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.common.auth.CurrentUser;
import com.example.bizagent.modules.modulecontainer.service.ModuleLoaderService;
import com.example.bizagent.modules.system.entity.SysMenu;
import com.example.bizagent.modules.system.entity.SysModule;
import com.example.bizagent.modules.system.entity.SysPermission;
import com.example.bizagent.modules.system.service.SysMenuService;
import com.example.bizagent.modules.system.service.SysModuleService;
import com.example.bizagent.modules.system.service.SysPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/module/container")
public class ModuleContainerController {

    private final ModuleLoaderService moduleLoaderService;
    private final SysModuleService sysModuleService;
    private final SysMenuService sysMenuService;
    private final SysPermissionService sysPermissionService;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModuleContainerController(ModuleLoaderService moduleLoaderService,
                                     SysModuleService sysModuleService,
                                     SysMenuService sysMenuService,
                                     SysPermissionService sysPermissionService,
                                     DataSource dataSource) {
        this.moduleLoaderService = moduleLoaderService;
        this.sysModuleService = sysModuleService;
        this.sysMenuService = sysMenuService;
        this.sysPermissionService = sysPermissionService;
        this.dataSource = dataSource;
    }

    @PostMapping("/load/{moduleId}")
    public ResponseEntity<Void> loadModule(@PathVariable("moduleId") Long moduleId) {
        CurrentUser.requirePermission("module:deploy");
        moduleLoaderService.loadModule(moduleId);
        return ResponseEntity.success("模块加载成功", null);
    }

    @PostMapping("/unload/{moduleId}")
    public ResponseEntity<Void> unloadModule(@PathVariable("moduleId") Long moduleId) {
        CurrentUser.requirePermission("module:deploy");
        moduleLoaderService.unloadModule(moduleId);
        return ResponseEntity.success("模块卸载成功", null);
    }

    @PostMapping("/reload/{moduleId}")
    public ResponseEntity<Void> reloadModule(@PathVariable("moduleId") Long moduleId) {
        CurrentUser.requirePermission("module:deploy");
        moduleLoaderService.reloadModule(moduleId);
        return ResponseEntity.success("模块重载成功", null);
    }

    @GetMapping("/status/{moduleId}")
    public ResponseEntity<Map<String, Object>> getModuleStatus(@PathVariable("moduleId") Long moduleId) {
        Map<String, Object> status = new HashMap<>();
        status.put("moduleId", moduleId);
        status.put("loaded", moduleLoaderService.isModuleLoaded(moduleId));
        return ResponseEntity.success(status);
    }

    @PostMapping("/mount/{moduleId}/project/{projectId}")
    public ResponseEntity<Void> mountToProject(@PathVariable("moduleId") Long moduleId, @PathVariable("projectId") Long projectId) {
        CurrentUser.requirePermission("module:deploy");
        moduleLoaderService.mountToProject(moduleId, projectId);
        return ResponseEntity.success("模块挂载到项目成功", null);
    }

    @GetMapping("/runtime/{moduleCode}")
    public ResponseEntity<Map<String, Object>> runtime(@PathVariable("moduleCode") String moduleCode,
                                                       @RequestParam(required = false) Long projectId) {
        Long currentProjectId = projectId == null || projectId <= 0 ? 1L : projectId;
        SysModule module = sysModuleService.getOne(new QueryWrapper<SysModule>()
                .eq("module_code", moduleCode)
                .eq("project_id", currentProjectId)
                .eq("del_flag", 0), false);
        if (module == null) {
            return ResponseEntity.error(404, "模块不存在");
        }
        List<SysMenu> menus = sysMenuService.list(new QueryWrapper<SysMenu>()
                .likeRight("path", "/module-runtime/" + moduleCode)
                .eq("project_id", currentProjectId));
        List<SysPermission> permissions = sysPermissionService.list(new QueryWrapper<SysPermission>()
                .likeRight("permission_code", moduleCode + ":")
                .eq("project_id", currentProjectId));
        Map<String, Object> runtime = new HashMap<>();
        runtime.put("module", module);
        runtime.put("menus", menus);
        runtime.put("permissions", permissions);
        runtime.put("userPermissions", loadUserPermissions(CurrentUser.id(), moduleCode, currentProjectId));
        runtime.put("design", readModuleDesign(module));
        runtime.put("loaded", moduleLoaderService.isModuleLoaded(module.getId()));
        return ResponseEntity.success(runtime);
    }

    private List<String> loadUserPermissions(Long userId, String moduleCode, Long projectId) {
        String sql = """
                SELECT DISTINCT p.permission_code
                FROM sys_user_role ur
                JOIN sys_role_permission rp ON rp.role_id = ur.role_id
                JOIN sys_permission p ON p.id = rp.permission_id
                WHERE ur.user_id = ? AND p.permission_code LIKE ? AND p.project_id = ? AND p.del_flag = 0
                """;
        List<String> permissions = new ArrayList<>();
        if (Long.valueOf(1L).equals(userId)) {
            List<SysPermission> modulePermissions = sysPermissionService.list(new QueryWrapper<SysPermission>()
                    .likeRight("permission_code", moduleCode + ":")
                    .eq("project_id", projectId)
                    .eq("del_flag", 0));
            for (SysPermission permission : modulePermissions) {
                permissions.add(permission.getPermissionCode());
            }
            return permissions;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, moduleCode + ":%");
            statement.setLong(3, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    permissions.add(resultSet.getString(1));
                }
            }
        } catch (Exception ignored) {
        }
        return permissions;
    }

    private Map<String, Object> readModuleDesign(SysModule module) {
        if (module.getDesignJson() != null && !module.getDesignJson().isBlank()) {
            try {
                return objectMapper.readValue(module.getDesignJson(), Map.class);
            } catch (Exception ignored) {
            }
        }
        try {
            Path cwd = Path.of("").toAbsolutePath().normalize();
            Path root = cwd.getFileName() != null && cwd.getFileName().toString().equalsIgnoreCase("server")
                    ? cwd.getParent()
                    : cwd;
            Path moduleJson = root.resolve("src").resolve("modules").resolve(module.getModuleCode()).resolve("module.json");
            if (!Files.exists(moduleJson)) {
                moduleJson = root.resolve("server").resolve("modules").resolve(module.getModuleCode()).resolve("module.json");
            }
            if (Files.exists(moduleJson)) {
                return objectMapper.readValue(moduleJson.toFile(), Map.class);
            }
        } catch (Exception ignored) {
        }
        return Map.of();
    }
}
