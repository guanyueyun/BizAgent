package com.example.bizagent.modules.equipment_inspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.equipment_inspection.entity.EquipmentInspectionMain;
import com.example.bizagent.modules.equipment_inspection.mapper.EquipmentInspectionMainMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EquipmentInspectionMainService extends ServiceImpl<EquipmentInspectionMainMapper, EquipmentInspectionMain> {

    public PageResponse<EquipmentInspectionMain> list(Integer pageNum, Integer pageSize, Long projectId, String deviceName, String deviceLocation, String inspectionPlan, String inspector, String status) {
        Page<EquipmentInspectionMain> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EquipmentInspectionMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EquipmentInspectionMain::getDelFlag, 0);
        wrapper.eq(EquipmentInspectionMain::getProjectId, projectId);
        if (deviceName != null && !deviceName.isEmpty()) {
            wrapper.like(EquipmentInspectionMain::getDeviceName, deviceName);
        }
        if (deviceLocation != null && !deviceLocation.isEmpty()) {
            wrapper.like(EquipmentInspectionMain::getDeviceLocation, deviceLocation);
        }
        if (inspectionPlan != null && !inspectionPlan.isEmpty()) {
            wrapper.like(EquipmentInspectionMain::getInspectionPlan, inspectionPlan);
        }
        if (inspector != null && !inspector.isEmpty()) {
            wrapper.like(EquipmentInspectionMain::getInspector, inspector);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(EquipmentInspectionMain::getStatus, status);
        }
        wrapper.orderByDesc(EquipmentInspectionMain::getCreateTime);
        Page<EquipmentInspectionMain> result = this.page(page, wrapper);
        return PageResponse.success(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ResponseEntity<EquipmentInspectionMain> detail(Long id, Long projectId) {
        LambdaQueryWrapper<EquipmentInspectionMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EquipmentInspectionMain::getId, id);
        wrapper.eq(EquipmentInspectionMain::getProjectId, projectId);
        wrapper.eq(EquipmentInspectionMain::getDelFlag, 0);
        EquipmentInspectionMain entity = this.getOne(wrapper);
        if (entity == null) {
            return ResponseEntity.error("巡检记录不存在");
        }
        return ResponseEntity.success(entity);
    }

    @Transactional
    public ResponseEntity<EquipmentInspectionMain> create(EquipmentInspectionMain entity, Long projectId) {
        if (entity.getDeviceName() == null || entity.getDeviceName().isEmpty()) {
            return ResponseEntity.error("设备名称不能为空");
        }
        if (entity.getDeviceLocation() == null || entity.getDeviceLocation().isEmpty()) {
            return ResponseEntity.error("设备位置不能为空");
        }
        if (entity.getInspector() == null || entity.getInspector().isEmpty()) {
            return ResponseEntity.error("巡检人不能为空");
        }
        if (entity.getInspectionTime() == null) {
            return ResponseEntity.error("巡检时间不能为空");
        }
        if (entity.getStatus() == null || entity.getStatus().isEmpty()) {
            return ResponseEntity.error("状态不能为空");
        }
        if (!entity.getStatus().matches("^(pending|completed|exception)$")) {
            return ResponseEntity.error("状态值无效，必须为 pending、completed 或 exception");
        }
        entity.setProjectId(projectId);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDelFlag(0);
        this.save(entity);
        return ResponseEntity.success(entity);
    }

    @Transactional
    public ResponseEntity<EquipmentInspectionMain> update(Long id, EquipmentInspectionMain entity, Long projectId) {
        LambdaQueryWrapper<EquipmentInspectionMain> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EquipmentInspectionMain::getId, id);
        queryWrapper.eq(EquipmentInspectionMain::getProjectId, projectId);
        queryWrapper.eq(EquipmentInspectionMain::getDelFlag, 0);
        EquipmentInspectionMain existing = this.getOne(queryWrapper);
        if (existing == null) {
            return ResponseEntity.error("巡检记录不存在");
        }
        if (entity.getDeviceName() != null && entity.getDeviceName().isEmpty()) {
            return ResponseEntity.error("设备名称不能为空");
        }
        if (entity.getDeviceLocation() != null && entity.getDeviceLocation().isEmpty()) {
            return ResponseEntity.error("设备位置不能为空");
        }
        if (entity.getInspector() != null && entity.getInspector().isEmpty()) {
            return ResponseEntity.error("巡检人不能为空");
        }
        if (entity.getStatus() != null && !entity.getStatus().matches("^(pending|completed|exception)$")) {
            return ResponseEntity.error("状态值无效，必须为 pending、completed 或 exception");
        }
        entity.setId(id);
        entity.setProjectId(projectId);
        entity.setUpdateTime(LocalDateTime.now());
        this.updateById(entity);
        EquipmentInspectionMain updated = this.getById(id);
        return ResponseEntity.success(updated);
    }

    @Transactional
    public ResponseEntity<Void> delete(Long id, Long projectId) {
        LambdaQueryWrapper<EquipmentInspectionMain> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EquipmentInspectionMain::getId, id);
        queryWrapper.eq(EquipmentInspectionMain::getProjectId, projectId);
        queryWrapper.eq(EquipmentInspectionMain::getDelFlag, 0);
        EquipmentInspectionMain existing = this.getOne(queryWrapper);
        if (existing == null) {
            return ResponseEntity.error("巡检记录不存在");
        }
        LambdaUpdateWrapper<EquipmentInspectionMain> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(EquipmentInspectionMain::getId, id);
        updateWrapper.set(EquipmentInspectionMain::getDelFlag, 1);
        this.update(updateWrapper);
        return ResponseEntity.success(null);
    }

    public PageResponse<EquipmentInspectionMain> exportList(Long projectId, String deviceName, String deviceLocation, String inspectionPlan, String inspector, String status) {
        LambdaQueryWrapper<EquipmentInspectionMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EquipmentInspectionMain::getDelFlag, 0);
        wrapper.eq(EquipmentInspectionMain::getProjectId, projectId);
        if (deviceName != null && !deviceName.isEmpty()) {
            wrapper.like(EquipmentInspectionMain::getDeviceName, deviceName);
        }
        if (deviceLocation != null && !deviceLocation.isEmpty()) {
            wrapper.like(EquipmentInspectionMain::getDeviceLocation, deviceLocation);
        }
        if (inspectionPlan != null && !inspectionPlan.isEmpty()) {
            wrapper.like(EquipmentInspectionMain::getInspectionPlan, inspectionPlan);
        }
        if (inspector != null && !inspector.isEmpty()) {
            wrapper.like(EquipmentInspectionMain::getInspector, inspector);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(EquipmentInspectionMain::getStatus, status);
        }
        wrapper.orderByDesc(EquipmentInspectionMain::getCreateTime);
        return PageResponse.success(this.list(wrapper), (long) this.list(wrapper).size(), 1L, (long) this.list(wrapper).size());
    }
}