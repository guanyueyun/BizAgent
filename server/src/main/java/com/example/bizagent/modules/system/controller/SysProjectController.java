
package com.example.bizagent.modules.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.system.entity.SysProject;
import com.example.bizagent.modules.system.service.SysProjectService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/project")
public class SysProjectController {

    private final SysProjectService sysProjectService;

    public SysProjectController(SysProjectService sysProjectService) {
        this.sysProjectService = sysProjectService;
    }

    @GetMapping("/list")
    public ResponseEntity<PageResponse<SysProject>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                        @RequestParam(defaultValue = "10") int pageSize) {
        IPage<SysProject> page = sysProjectService.page(new Page<>(pageNum, pageSize));
        return ResponseEntity.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SysProject> getById(@PathVariable Long id) {
        return ResponseEntity.success(sysProjectService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SysProject> create(@RequestBody SysProject project) {
        sysProjectService.save(project);
        return ResponseEntity.success("创建成功", project);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SysProject> update(@PathVariable Long id, @RequestBody SysProject project) {
        project.setId(id);
        sysProjectService.updateById(project);
        return ResponseEntity.success("更新成功", project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sysProjectService.removeById(id);
        return ResponseEntity.success(null);
    }
}
