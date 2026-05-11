package com.example.bizagent.modules.approval.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.approval.entity.ApprovalEntity;
import com.example.bizagent.modules.approval.service.ApprovalService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/biz/approval")
public class ApprovalController {

    private final ApprovalService service;

    public ApprovalController(ApprovalService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public ResponseEntity<PageResponse<ApprovalEntity>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<ApprovalEntity> page = service.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<ApprovalEntity>().eq(ApprovalEntity::getDelFlag, 0).orderByDesc(ApprovalEntity::getCreateTime));
        return ResponseEntity.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApprovalEntity> get(@PathVariable Long id) {
        return ResponseEntity.success(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ApprovalEntity> create(@RequestBody ApprovalEntity entity) {
        service.save(entity);
        return ResponseEntity.success("新增成功", entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApprovalEntity> update(@PathVariable Long id, @RequestBody ApprovalEntity entity) {
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
