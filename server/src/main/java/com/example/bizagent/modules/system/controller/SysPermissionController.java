
package com.example.bizagent.modules.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.system.entity.SysPermission;
import com.example.bizagent.modules.system.service.SysPermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/permission")
public class SysPermissionController {

    private final SysPermissionService sysPermissionService;

    public SysPermissionController(SysPermissionService sysPermissionService) {
        this.sysPermissionService = sysPermissionService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('permission:list')")
    public ResponseEntity<PageResponse<SysPermission>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                           @RequestParam(defaultValue = "10") int pageSize) {
        IPage<SysPermission> page = sysPermissionService.page(new Page<>(pageNum, pageSize));
        return ResponseEntity.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:list')")
    public ResponseEntity<SysPermission> getById(@PathVariable Long id) {
        return ResponseEntity.success(sysPermissionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('permission:add')")
    public ResponseEntity<SysPermission> create(@RequestBody SysPermission permission) {
        sysPermissionService.save(permission);
        return ResponseEntity.success("创建成功", permission);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:edit')")
    public ResponseEntity<SysPermission> update(@PathVariable Long id, @RequestBody SysPermission permission) {
        permission.setId(id);
        sysPermissionService.updateById(permission);
        return ResponseEntity.success("更新成功", permission);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sysPermissionService.removeById(id);
        return ResponseEntity.success(null);
    }
}
