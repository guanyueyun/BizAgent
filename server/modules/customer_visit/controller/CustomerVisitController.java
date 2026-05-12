package com.example.bizagent.modules.customer_visit.controller;

import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.customer_visit.entity.CustomerVisitMain;
import com.example.bizagent.modules.customer_visit.service.CustomerVisitMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/biz/customer_visit")
public class CustomerVisitMainController {

    @Autowired
    private CustomerVisitMainService customerVisitMainService;

    @GetMapping("/list")
    public PageResponse<CustomerVisitMain> list(
            @RequestParam Long projectId,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String visitMethod,
            @RequestParam(required = false) String follower,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return customerVisitMainService.list(projectId, customerName, visitMethod, follower, status, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerVisitMain> detail(@PathVariable Long id, @RequestParam Long projectId) {
        return customerVisitMainService.detail(id, projectId);
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody CustomerVisitMain entity, @RequestParam Long projectId) {
        return customerVisitMainService.create(entity, projectId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@RequestBody CustomerVisitMain entity, @PathVariable Long id, @RequestParam Long projectId) {
        return customerVisitMainService.update(entity, id, projectId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id, @RequestParam Long projectId) {
        return customerVisitMainService.delete(id, projectId);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics(@RequestParam Long projectId) {
        return customerVisitMainService.statistics(projectId);
    }
}