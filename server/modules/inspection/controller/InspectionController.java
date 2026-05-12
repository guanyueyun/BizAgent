package com.example.bizagent.modules.inspection.controller;

import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.inspection.entity.InspectionMain;
import com.example.bizagent.modules.inspection.service.InspectionMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/biz/inspection")
public class InspectionMainController {

    @Autowired
    private InspectionMainService inspectionMainService;

    @GetMapping("/list")
    public PageResponse<InspectionMain> list(@RequestParam("projectId") Long projectId,
                                             @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                             @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                             @RequestParam(value = "deviceName", required = false) String deviceName,
                                             @RequestParam(value = "deviceLocation", required = false) String deviceLocation,
                                             @RequestParam(value = "inspector", required = false) String inspector,
                                             @RequestParam(value = "status", required = false) String status,
                                             @RequestParam(value = "inspectionTime", required = false) String inspectionTime,
                                             @RequestParam(value = "rectificationStatus", required = false) String rectificationStatus) {
        return inspectionMainService.list(projectId, pageNum, pageSize, deviceName, deviceLocation, inspector, status, inspectionTime, rectificationStatus);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectionMain> detail(@PathVariable("id") Long id,
                                                 @RequestParam("projectId") Long projectId) {
        return inspectionMainService.detail(id, projectId);
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody InspectionMain entity) {
        return inspectionMainService.create(entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Boolean> update(@PathVariable("id") Long id,
                                          @RequestBody InspectionMain entity,
                                          @RequestParam("projectId") Long projectId) {
        return inspectionMainService.update(id, entity, projectId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> delete(@PathVariable("id") Long id,
                                          @RequestParam("projectId") Long projectId) {
        return inspectionMainService.delete(id, projectId);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics(@RequestParam("projectId") Long projectId,
                                                          @RequestParam("startTime") String startTime,
                                                          @RequestParam("endTime") String endTime) {
        return inspectionMainService.statistics(projectId, startTime, endTime);
    }
}