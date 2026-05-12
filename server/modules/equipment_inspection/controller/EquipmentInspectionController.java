package com.example.bizagent.modules.equipment_inspection.controller;

import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.equipment_inspection.entity.EquipmentInspectionMain;
import com.example.bizagent.modules.equipment_inspection.service.EquipmentInspectionMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/biz/equipment_inspection")
public class EquipmentInspectionMainController {

    @Autowired
    private EquipmentInspectionMainService equipmentInspectionMainService;

    @GetMapping("/list")
    public PageResponse<EquipmentInspectionMain> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long projectId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceLocation,
            @RequestParam(required = false) String inspectionPlan,
            @RequestParam(required = false) String inspector,
            @RequestParam(required = false) String status) {
        return equipmentInspectionMainService.list(pageNum, pageSize, projectId, deviceName, deviceLocation, inspectionPlan, inspector, status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentInspectionMain> detail(@PathVariable Long id, @RequestParam Long projectId) {
        return equipmentInspectionMainService.detail(id, projectId);
    }

    @PostMapping
    public ResponseEntity<EquipmentInspectionMain> create(@RequestBody EquipmentInspectionMain entity, @RequestParam Long projectId) {
        return equipmentInspectionMainService.create(entity, projectId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipmentInspectionMain> update(@PathVariable Long id, @RequestBody EquipmentInspectionMain entity, @RequestParam Long projectId) {
        return equipmentInspectionMainService.update(id, entity, projectId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long projectId) {
        return equipmentInspectionMainService.delete(id, projectId);
    }

    @GetMapping("/export")
    public PageResponse<EquipmentInspectionMain> export(
            @RequestParam Long projectId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceLocation,
            @RequestParam(required = false) String inspectionPlan,
            @RequestParam(required = false) String inspector,
            @RequestParam(required = false) String status) {
        return equipmentInspectionMainService.exportList(projectId, deviceName, deviceLocation, inspectionPlan, inspector, status);
    }
}