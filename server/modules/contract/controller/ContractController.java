package com.example.bizagent.modules.contract.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.contract.entity.ContractEntity;
import com.example.bizagent.modules.contract.service.ContractService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/biz/contract")
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public ResponseEntity<PageResponse<ContractEntity>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<ContractEntity> page = service.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<ContractEntity>().eq(ContractEntity::getDelFlag, 0).orderByDesc(ContractEntity::getCreateTime));
        return ResponseEntity.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractEntity> get(@PathVariable Long id) {
        return ResponseEntity.success(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ContractEntity> create(@RequestBody ContractEntity entity) {
        service.save(entity);
        return ResponseEntity.success("新增成功", entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContractEntity> update(@PathVariable Long id, @RequestBody ContractEntity entity) {
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
