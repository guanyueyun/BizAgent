package com.example.bizagent.modules.customer.controller;

import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.customer.entity.CustomerMain;
import com.example.bizagent.modules.customer.service.CustomerMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/biz/customer")
public class CustomerMainController {

    @Autowired
    private CustomerMainService customerMainService;

    @GetMapping("/list")
    public PageResponse<CustomerMain> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long projectId,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String contactPerson,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String followUpPerson,
            @RequestParam(required = false) String status) {
        return customerMainService.list(pageNum, pageSize, projectId, customerName, contactPerson, contactPhone, industry, source, followUpPerson, status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerMain> detail(@PathVariable Long id, @RequestParam Long projectId) {
        return customerMainService.detail(id, projectId);
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody CustomerMain entity, @RequestParam Long projectId) {
        return customerMainService.create(entity, projectId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Boolean> update(@PathVariable Long id, @RequestBody CustomerMain entity, @RequestParam Long projectId) {
        entity.setId(id);
        return customerMainService.update(entity, projectId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> delete(@PathVariable Long id, @RequestParam Long projectId) {
        return customerMainService.delete(id, projectId);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics(@RequestParam Long projectId) {
        return customerMainService.statistics(projectId);
    }
}