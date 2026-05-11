
package com.example.bizagent.modules.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.system.entity.SysMenu;
import com.example.bizagent.modules.system.service.SysMenuService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/menu")
public class SysMenuController {

    private final SysMenuService sysMenuService;

    public SysMenuController(SysMenuService sysMenuService) {
        this.sysMenuService = sysMenuService;
    }

    @GetMapping("/list")
    public ResponseEntity<PageResponse<SysMenu>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) Long projectId) {
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<SysMenu>().eq("del_flag", 0);
        if (projectId != null && projectId > 0) {
            wrapper.and(query -> query.eq("project_id", projectId).or().eq("project_id", 0));
        }
        IPage<SysMenu> page = sysMenuService.page(new Page<>(pageNum, pageSize), wrapper);
        return ResponseEntity.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/tree")
    public ResponseEntity<List<SysMenu>> tree() {
        List<SysMenu> menus = sysMenuService.list();
        return ResponseEntity.success(menus);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SysMenu> getById(@PathVariable Long id) {
        return ResponseEntity.success(sysMenuService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SysMenu> create(@RequestBody SysMenu menu) {
        sysMenuService.save(menu);
        return ResponseEntity.success("创建成功", menu);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SysMenu> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        sysMenuService.updateById(menu);
        return ResponseEntity.success("更新成功", menu);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sysMenuService.removeById(id);
        return ResponseEntity.success(null);
    }
}
