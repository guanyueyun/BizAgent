
package com.example.bizagent.modules.modulecontainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bizagent.modules.modulecontainer.service.ModuleLoaderService;
import com.example.bizagent.modules.system.entity.SysMenu;
import com.example.bizagent.modules.system.entity.SysModule;
import com.example.bizagent.modules.system.entity.SysPermission;
import com.example.bizagent.modules.system.service.SysMenuService;
import com.example.bizagent.modules.system.service.SysModuleService;
import com.example.bizagent.modules.system.service.SysPermissionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModuleLoaderServiceImpl implements ModuleLoaderService {

    private final SysModuleService sysModuleService;
    private final SysMenuService sysMenuService;
    private final SysPermissionService sysPermissionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Long, Boolean> loadedModules = new HashMap<>();

    public ModuleLoaderServiceImpl(SysModuleService sysModuleService,
                                   SysMenuService sysMenuService,
                                   SysPermissionService sysPermissionService) {
        this.sysModuleService = sysModuleService;
        this.sysMenuService = sysMenuService;
        this.sysPermissionService = sysPermissionService;
    }

    @Override
    public void loadModule(Long moduleId) {
        SysModule module = sysModuleService.getById(moduleId);
        if (module != null) {
            loadedModules.put(moduleId, true);
            module.setStatus(1);
            module.setLifecycle(2);
            sysModuleService.updateById(module);
            mountPermissions(moduleId);
            mountMenus(moduleId);
            mountRoutes(moduleId);
        }
    }

    @Override
    public void unloadModule(Long moduleId) {
        loadedModules.remove(moduleId);
        SysModule module = sysModuleService.getById(moduleId);
        if (module != null) {
            module.setStatus(0);
            module.setLifecycle(4);
            sysModuleService.updateById(module);
        }
    }

    @Override
    public void reloadModule(Long moduleId) {
        unloadModule(moduleId);
        loadModule(moduleId);
    }

    @Override
    public boolean isModuleLoaded(Long moduleId) {
        SysModule module = sysModuleService.getById(moduleId);
        return loadedModules.getOrDefault(moduleId, false)
                || (module != null && Integer.valueOf(2).equals(module.getLifecycle()) && Integer.valueOf(1).equals(module.getStatus()));
    }

    @Override
    public void mountPermissions(Long moduleId) {
        SysModule module = sysModuleService.getById(moduleId);
        if (module == null || module.getFrontPath() == null) {
            return;
        }
        
        try {
            File permissionsFile = resolveModuleFile(module, "permissions.json");
            if (!permissionsFile.exists()) {
                return;
            }

            List<PermissionDTO> permissions = objectMapper.readValue(
                    permissionsFile, 
                    new TypeReference<List<PermissionDTO>>() {}
            );

            for (PermissionDTO dto : permissions) {
                SysPermission existing = sysPermissionService.getOne(
                        new QueryWrapper<SysPermission>().eq("permission_code", dto.permissionCode()), 
                        false
                );
                
                if (existing == null) {
                    SysPermission permission = new SysPermission();
                    permission.setPermissionCode(dto.permissionCode());
                    permission.setPermissionName(dto.permissionName());
                    permission.setModuleName(dto.moduleName());
                    permission.setDescription(dto.description());
                    permission.setProjectId(module.getProjectId());
                    sysPermissionService.save(permission);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("挂载模块权限失败", e);
        }
    }

    @Override
    public void mountMenus(Long moduleId) {
        SysModule module = sysModuleService.getById(moduleId);
        if (module == null || module.getFrontPath() == null) {
            return;
        }

        try {
            File menusFile = resolveModuleFile(module, "menus.json");
            if (!menusFile.exists()) {
                return;
            }
            List<MenuDTO> menus = objectMapper.readValue(menusFile, new TypeReference<List<MenuDTO>>() {});
            for (MenuDTO dto : menus) {
                SysMenu menu = sysMenuService.getOne(new QueryWrapper<SysMenu>()
                        .eq("path", dto.path())
                        .eq("project_id", module.getProjectId()), false);
                if (menu == null) {
                    menu = new SysMenu();
                }
                menu.setMenuName(dto.menuName());
                menu.setPath(dto.path());
                menu.setComponent(dto.component());
                menu.setParentId(dto.parentId() == null ? 0L : dto.parentId());
                menu.setIcon(dto.icon());
                menu.setSortOrder(dto.sortOrder() == null ? 100 : dto.sortOrder());
                menu.setMenuType(dto.menuType() == null ? 2 : dto.menuType());
                menu.setPermission(module.getModuleCode() + ":list");
                menu.setVisible(1);
                menu.setStatus(1);
                menu.setProjectId(module.getProjectId());
                if (menu.getId() == null) {
                    sysMenuService.save(menu);
                } else {
                    sysMenuService.updateById(menu);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("挂载模块菜单失败", e);
        }
    }

    @Override
    public void mountRoutes(Long moduleId) {
        SysModule module = sysModuleService.getById(moduleId);
        if (module == null || module.getFrontPath() == null) {
            return;
        }

        try {
            File routesFile = resolveModuleFile(module, "routes.json");
            if (!routesFile.exists()) {
                return;
            }
            String routesJson = Files.readString(routesFile.toPath());
            if (module.getDesignJson() == null || !module.getDesignJson().contains("\"pages\"")) {
                module.setDesignJson("{\"pages\":" + routesJson + "}");
                sysModuleService.updateById(module);
            }
        } catch (Exception e) {
            throw new IllegalStateException("挂载模块路由失败", e);
        }
    }

    @Override
    public void mountToProject(Long moduleId, Long projectId) {
        SysModule module = sysModuleService.getById(moduleId);
        if (module != null) {
            module.setProjectId(projectId);
            sysModuleService.updateById(module);
        }
    }

    private File resolveModuleFile(SysModule module, String filename) {
        Path path = Path.of(module.getFrontPath()).resolve(filename);
        if (Files.exists(path)) {
            return path.toFile();
        }
        Path root = projectRoot();
        String normalized = module.getFrontPath().replace("\\", "/").replaceFirst("^/+", "");
        path = root.resolve(normalized).resolve(filename);
        if (Files.exists(path)) {
            return path.toFile();
        }
        return root.resolve("src").resolve("modules").resolve(module.getModuleCode()).resolve(filename).toFile();
    }

    private Path projectRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        return cwd.getFileName() != null && cwd.getFileName().toString().equalsIgnoreCase("server")
                ? cwd.getParent()
                : cwd;
    }

    private record PermissionDTO(String permissionCode, String permissionName, String moduleName, String description) {}

    private record MenuDTO(String menuName, String path, String component, Long parentId, String icon, Integer sortOrder, Integer menuType) {}
}
