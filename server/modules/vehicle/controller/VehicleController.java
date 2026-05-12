package com.example.bizagent.modules.vehicle.controller;

import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.vehicle.entity.VehicleMain;
import com.example.bizagent.modules.vehicle.service.VehicleMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/biz/vehicle")
public class VehicleMainController {

    @Autowired
    private VehicleMainService vehicleMainService;

    @GetMapping("/list")
    public PageResponse<VehicleMain> listVehicles(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long projectId,
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String driver) {
        return vehicleMainService.listVehicles(pageNum, pageSize, projectId, plateNumber, brand, model, status, approvalStatus, driver);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleMain> getVehicleDetail(@PathVariable Long id, @RequestParam Long projectId) {
        return vehicleMainService.getVehicleDetail(id, projectId);
    }

    @PostMapping
    public ResponseEntity<VehicleMain> createVehicle(@RequestBody VehicleMain vehicle, @RequestParam Long projectId) {
        return vehicleMainService.createVehicle(vehicle, projectId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleMain> updateVehicle(@PathVariable Long id, @RequestBody VehicleMain vehicle, @RequestParam Long projectId) {
        return vehicleMainService.updateVehicle(id, vehicle, projectId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id, @RequestParam Long projectId) {
        return vehicleMainService.deleteVehicle(id, projectId);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@RequestParam Long projectId) {
        return vehicleMainService.getStatistics(projectId);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submitForApproval(@PathVariable Long id, @RequestParam Long projectId) {
        return vehicleMainService.submitForApproval(id, projectId);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveVehicle(
            @PathVariable Long id,
            @RequestParam Long projectId,
            @RequestParam String approvalAction,
            @RequestParam(required = false) String approvalComment) {
        return vehicleMainService.approveVehicle(id, projectId, approvalAction, approvalComment);
    }
}