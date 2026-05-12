package com.example.bizagent.modules.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.vehicle.entity.VehicleMain;
import com.example.bizagent.modules.vehicle.mapper.VehicleMainMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VehicleMainService extends ServiceImpl<VehicleMainMapper, VehicleMain> {

    public PageResponse<VehicleMain> listVehicles(Integer pageNum, Integer pageSize, Long projectId, String plateNumber, String brand, String model, String status, String approvalStatus, String driver) {
        Page<VehicleMain> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<VehicleMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleMain::getDelFlag, 0);
        wrapper.eq(VehicleMain::getProjectId, projectId);
        if (StringUtils.hasText(plateNumber)) {
            wrapper.like(VehicleMain::getPlateNumber, plateNumber);
        }
        if (StringUtils.hasText(brand)) {
            wrapper.like(VehicleMain::getBrand, brand);
        }
        if (StringUtils.hasText(model)) {
            wrapper.like(VehicleMain::getModel, model);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(VehicleMain::getStatus, status);
        }
        if (StringUtils.hasText(approvalStatus)) {
            wrapper.eq(VehicleMain::getApprovalStatus, approvalStatus);
        }
        if (StringUtils.hasText(driver)) {
            wrapper.like(VehicleMain::getDriver, driver);
        }
        wrapper.orderByDesc(VehicleMain::getCreateTime);
        IPage<VehicleMain> result = this.baseMapper.selectPage(page, wrapper);
        PageResponse<VehicleMain> response = new PageResponse<>();
        response.setTotal(result.getTotal());
        response.setRecords(result.getRecords());
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        return response;
    }

    public ResponseEntity<VehicleMain> getVehicleDetail(Long id, Long projectId) {
        LambdaQueryWrapper<VehicleMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleMain::getId, id);
        wrapper.eq(VehicleMain::getDelFlag, 0);
        wrapper.eq(VehicleMain::getProjectId, projectId);
        VehicleMain vehicle = this.baseMapper.selectOne(wrapper);
        if (vehicle == null) {
            return ResponseEntity.error("车辆不存在");
        }
        return ResponseEntity.success(vehicle);
    }

    public ResponseEntity<VehicleMain> createVehicle(VehicleMain vehicle, Long projectId) {
        vehicle.setProjectId(projectId);
        vehicle.setDelFlag(0);
        vehicle.setCreateTime(LocalDateTime.now());
        vehicle.setUpdateTime(LocalDateTime.now());
        if (vehicle.getApprovalStatus() == null) {
            vehicle.setApprovalStatus("draft");
        }
        if (vehicle.getMileage() == null) {
            vehicle.setMileage("0.00");
        }
        this.baseMapper.insert(vehicle);
        return ResponseEntity.success(vehicle);
    }

    public ResponseEntity<VehicleMain> updateVehicle(Long id, VehicleMain vehicle, Long projectId) {
        LambdaQueryWrapper<VehicleMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleMain::getId, id);
        wrapper.eq(VehicleMain::getDelFlag, 0);
        wrapper.eq(VehicleMain::getProjectId, projectId);
        VehicleMain existing = this.baseMapper.selectOne(wrapper);
        if (existing == null) {
            return ResponseEntity.error("车辆不存在");
        }
        vehicle.setId(id);
        vehicle.setProjectId(projectId);
        vehicle.setUpdateTime(LocalDateTime.now());
        this.baseMapper.updateById(vehicle);
        return ResponseEntity.success(this.baseMapper.selectById(id));
    }

    public ResponseEntity<Void> deleteVehicle(Long id, Long projectId) {
        LambdaQueryWrapper<VehicleMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleMain::getId, id);
        wrapper.eq(VehicleMain::getDelFlag, 0);
        wrapper.eq(VehicleMain::getProjectId, projectId);
        VehicleMain existing = this.baseMapper.selectOne(wrapper);
        if (existing == null) {
            return ResponseEntity.error("车辆不存在");
        }
        VehicleMain update = new VehicleMain();
        update.setId(id);
        update.setDelFlag(1);
        update.setUpdateTime(LocalDateTime.now());
        this.baseMapper.updateById(update);
        return ResponseEntity.success(null);
    }

    public ResponseEntity<Map<String, Object>> getStatistics(Long projectId) {
        List<VehicleMain> allVehicles = this.baseMapper.selectList(
                new LambdaQueryWrapper<VehicleMain>()
                        .eq(VehicleMain::getDelFlag, 0)
                        .eq(VehicleMain::getProjectId, projectId)
        );
        Map<String, Long> statusCount = new HashMap<>();
        Map<String, Long> brandCount = new HashMap<>();
        for (VehicleMain v : allVehicles) {
            String status = v.getStatus();
            statusCount.merge(status, 1L, Long::sum);
            String brand = v.getBrand();
            if (brand != null) {
                brandCount.merge(brand, 1L, Long::sum);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", allVehicles.size());
        result.put("statusCount", statusCount);
        result.put("brandCount", brandCount);
        return ResponseEntity.success(result);
    }

    public ResponseEntity<Void> submitForApproval(Long id, Long projectId) {
        LambdaQueryWrapper<VehicleMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleMain::getId, id);
        wrapper.eq(VehicleMain::getDelFlag, 0);
        wrapper.eq(VehicleMain::getProjectId, projectId);
        VehicleMain existing = this.baseMapper.selectOne(wrapper);
        if (existing == null) {
            return ResponseEntity.error("车辆不存在");
        }
        if (!"draft".equals(existing.getApprovalStatus())) {
            return ResponseEntity.error("只有草稿状态的车辆可以提交审批");
        }
        VehicleMain update = new VehicleMain();
        update.setId(id);
        update.setApprovalStatus("pending");
        update.setUpdateTime(LocalDateTime.now());
        this.baseMapper.updateById(update);
        return ResponseEntity.success(null);
    }

    public ResponseEntity<Void> approveVehicle(Long id, Long projectId, String approvalAction, String approvalComment) {
        LambdaQueryWrapper<VehicleMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleMain::getId, id);
        wrapper.eq(VehicleMain::getDelFlag, 0);
        wrapper.eq(VehicleMain::getProjectId, projectId);
        VehicleMain existing = this.baseMapper.selectOne(wrapper);
        if (existing == null) {
            return ResponseEntity.error("车辆不存在");
        }
        if (!"pending".equals(existing.getApprovalStatus())) {
            return ResponseEntity.error("只有待审批状态的车辆可以审批");
        }
        if (!"approved".equals(approvalAction) && !"rejected".equals(approvalAction)) {
            return ResponseEntity.error("审批操作无效，必须为 approved 或 rejected");
        }
        VehicleMain update = new VehicleMain();
        update.setId(id);
        update.setApprovalStatus(approvalAction);
        update.setUpdateTime(LocalDateTime.now());
        this.baseMapper.updateById(update);
        return ResponseEntity.success(null);
    }
}