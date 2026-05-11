package com.example.bizagent.modules.module.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.module.entity.ModuleEntity;
import com.example.bizagent.modules.module.service.ModuleService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/biz/module")
public class ModuleController {

    private final ModuleService service;

    public ModuleController(ModuleService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public ResponseEntity<PageResponse<ModuleEntity>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<ModuleEntity> page = service.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<ModuleEntity>().eq(ModuleEntity::getDelFlag, 0).orderByDesc(ModuleEntity::getCreateTime));
        return ResponseEntity.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModuleEntity> get(@PathVariable Long id) {
        return ResponseEntity.success(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ModuleEntity> create(@RequestBody ModuleEntity entity) {
        service.save(entity);
        return ResponseEntity.success("新增成功", entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModuleEntity> update(@PathVariable Long id, @RequestBody ModuleEntity entity) {
        entity.setId(id);
        service.updateById(entity);
        return ResponseEntity.success("修改成功", entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return ResponseEntity.success("删除成功");
    }
}
