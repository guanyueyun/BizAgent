package com.example.bizagent.modules.inspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.inspection.entity.InspectionMain;
import com.example.bizagent.modules.inspection.mapper.InspectionMainMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class InspectionMainService extends ServiceImpl<InspectionMainMapper, InspectionMain> {

    public PageResponse<InspectionMain> list(Long projectId, Integer pageNum, Integer pageSize,
                                             String deviceName, String deviceLocation,
                                             String inspector, String status,
                                             String inspectionTime, String rectificationStatus) {
        Page<InspectionMain> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<InspectionMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionMain::getDelFlag, 0);
        wrapper.eq(InspectionMain::getProjectId, projectId);
        if (StringUtils.isNotBlank(deviceName)) {
            wrapper.like(InspectionMain::getDeviceName, deviceName);
        }
        if (StringUtils.isNotBlank(deviceLocation)) {
            wrapper.like(InspectionMain::getDeviceLocation, deviceLocation);
        }
        if (StringUtils.isNotBlank(inspector)) {
            wrapper.like(InspectionMain::getInspector, inspector);
        }
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(InspectionMain::getStatus, status);
        }
        if (StringUtils.isNotBlank(inspectionTime)) {
            wrapper.eq(InspectionMain::getInspectionTime, inspectionTime);
        }
        if (StringUtils.isNotBlank(rectificationStatus)) {
            wrapper.eq(InspectionMain::getRectificationStatus, rectificationStatus);
        }
        wrapper.orderByDesc(InspectionMain::getId);
        IPage<InspectionMain> iPage = baseMapper.selectPage(page, wrapper);
        return PageResponse.success(iPage.getRecords(), iPage.getTotal(), iPage.getCurrent(), iPage.getSize());
    }

    public ResponseEntity<InspectionMain> detail(Long id, Long projectId) {
        InspectionMain entity = baseMapper.selectById(id);
        if (entity == null || entity.getDelFlag() == 1 || !entity.getProjectId().equals(projectId)) {
            return ResponseEntity.error("巡检记录不存在");
        }
        return ResponseEntity.success(entity);
    }

    public ResponseEntity<Long> create(InspectionMain entity) {
        if (entity.getProjectId() == null) {
            return ResponseEntity.error("项目ID不能为空");
        }
        if (StringUtils.isBlank(entity.getDeviceName())) {
            return ResponseEntity.error("设备名称不能为空");
        }
        if (StringUtils.isBlank(entity.getDeviceLocation())) {
            return ResponseEntity.error("设备位置不能为空");
        }
        if (StringUtils.isBlank(entity.getPlanName())) {
            return ResponseEntity.error("巡检计划名称不能为空");
        }
        if (StringUtils.isBlank(entity.getInspector())) {
            return ResponseEntity.error("巡检人不能为空");
        }
        if (StringUtils.isBlank(entity.getInspectionTime())) {
            return ResponseEntity.error("巡检时间不能为空");
        }
        if (StringUtils.isBlank(entity.getStatus())) {
            return ResponseEntity.error("状态不能为空");
        }
        entity.setDelFlag(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(entity);
        return ResponseEntity.success(entity.getId());
    }

    public ResponseEntity<Boolean> update(Long id, InspectionMain entity, Long projectId) {
        InspectionMain existing = baseMapper.selectById(id);
        if (existing == null || existing.getDelFlag() == 1 || !existing.getProjectId().equals(projectId)) {
            return ResponseEntity.error("巡检记录不存在");
        }
        if (StringUtils.isBlank(entity.getDeviceName())) {
            return ResponseEntity.error("设备名称不能为空");
        }
        if (StringUtils.isBlank(entity.getDeviceLocation())) {
            return ResponseEntity.error("设备位置不能为空");
        }
        if (StringUtils.isBlank(entity.getPlanName())) {
            return ResponseEntity.error("巡检计划名称不能为空");
        }
        if (StringUtils.isBlank(entity.getInspector())) {
            return ResponseEntity.error("巡检人不能为空");
        }
        if (StringUtils.isBlank(entity.getInspectionTime())) {
            return ResponseEntity.error("巡检时间不能为空");
        }
        if (StringUtils.isBlank(entity.getStatus())) {
            return ResponseEntity.error("状态不能为空");
        }
        entity.setId(id);
        entity.setProjectId(projectId);
        entity.setUpdateTime(LocalDateTime.now());
        boolean updated = baseMapper.updateById(entity) > 0;
        return ResponseEntity.success(updated);
    }

    public ResponseEntity<Boolean> delete(Long id, Long projectId) {
        InspectionMain existing = baseMapper.selectById(id);
        if (existing == null || existing.getDelFlag() == 1 || !existing.getProjectId().equals(projectId)) {
            return ResponseEntity.error("巡检记录不存在");
        }
        InspectionMain entity = new InspectionMain();
        entity.setId(id);
        entity.setDelFlag(1);
        entity.setUpdateTime(LocalDateTime.now());
        boolean deleted = baseMapper.updateById(entity) > 0;
        return ResponseEntity.success(deleted);
    }

    public ResponseEntity<Map<String, Object>> statistics(Long projectId, String startTime, String endTime) {
        if (projectId == null) {
            return ResponseEntity.error("项目ID不能为空");
        }
        if (StringUtils.isBlank(startTime) || StringUtils.isBlank(endTime)) {
            return ResponseEntity.error("开始时间和结束时间不能为空");
        }
        Map<String, Object> stats = baseMapper.selectStatistics(projectId, startTime, endTime);
        if (stats == null) {
            stats = new HashMap<>();
            stats.put("pendingCount", 0);
            stats.put("completedCount", 0);
            stats.put("abnormalCount", 0);
            stats.put("rectificationPendingCount", 0);
            stats.put("rectificationCompletedCount", 0);
        }
        return ResponseEntity.success(stats);
    }
}