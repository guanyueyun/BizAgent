package com.example.bizagent.modules.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.customer.entity.CustomerMain;
import com.example.bizagent.modules.customer.mapper.CustomerMainMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerMainService extends ServiceImpl<CustomerMainMapper, CustomerMain> {

    public PageResponse<CustomerMain> list(Integer pageNum, Integer pageSize, Long projectId, String customerName, String contactPerson, String contactPhone, String industry, String source, String followUpPerson, String status) {
        Page<CustomerMain> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CustomerMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerMain::getProjectId, projectId);
        wrapper.eq(CustomerMain::getDelFlag, 0);
        if (StringUtils.hasText(customerName)) {
            wrapper.like(CustomerMain::getCustomerName, customerName);
        }
        if (StringUtils.hasText(contactPerson)) {
            wrapper.like(CustomerMain::getContactPerson, contactPerson);
        }
        if (StringUtils.hasText(contactPhone)) {
            wrapper.like(CustomerMain::getContactPhone, contactPhone);
        }
        if (StringUtils.hasText(industry)) {
            wrapper.eq(CustomerMain::getIndustry, industry);
        }
        if (StringUtils.hasText(source)) {
            wrapper.eq(CustomerMain::getSource, source);
        }
        if (StringUtils.hasText(followUpPerson)) {
            wrapper.like(CustomerMain::getFollowUpPerson, followUpPerson);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(CustomerMain::getStatus, status);
        }
        wrapper.orderByDesc(CustomerMain::getCreateTime);
        IPage<CustomerMain> result = baseMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ResponseEntity<CustomerMain> detail(Long id, Long projectId) {
        LambdaQueryWrapper<CustomerMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerMain::getId, id);
        wrapper.eq(CustomerMain::getProjectId, projectId);
        wrapper.eq(CustomerMain::getDelFlag, 0);
        CustomerMain entity = baseMapper.selectOne(wrapper);
        if (entity == null) {
            return ResponseEntity.fail("客户不存在");
        }
        return ResponseEntity.success(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Long> create(CustomerMain entity, Long projectId) {
        entity.setId(null);
        entity.setProjectId(projectId);
        entity.setDelFlag(0);
        if (!StringUtils.hasText(entity.getCustomerName())) {
            return ResponseEntity.fail("客户名称不能为空");
        }
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("potential");
        }
        if (baseMapper.insert(entity) > 0) {
            return ResponseEntity.success(entity.getId());
        }
        return ResponseEntity.fail("新增客户失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Boolean> update(CustomerMain entity, Long projectId) {
        LambdaQueryWrapper<CustomerMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerMain::getId, entity.getId());
        wrapper.eq(CustomerMain::getProjectId, projectId);
        wrapper.eq(CustomerMain::getDelFlag, 0);
        CustomerMain existing = baseMapper.selectOne(wrapper);
        if (existing == null) {
            return ResponseEntity.fail("客户不存在或不属于当前项目");
        }
        if (!StringUtils.hasText(entity.getCustomerName())) {
            return ResponseEntity.fail("客户名称不能为空");
        }
        entity.setProjectId(null);
        entity.setDelFlag(null);
        entity.setCreateTime(null);
        entity.setCreateBy(null);
        if (baseMapper.updateById(entity) > 0) {
            return ResponseEntity.success(true);
        }
        return ResponseEntity.fail("更新客户失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Boolean> delete(Long id, Long projectId) {
        LambdaQueryWrapper<CustomerMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerMain::getId, id);
        wrapper.eq(CustomerMain::getProjectId, projectId);
        wrapper.eq(CustomerMain::getDelFlag, 0);
        CustomerMain existing = baseMapper.selectOne(wrapper);
        if (existing == null) {
            return ResponseEntity.fail("客户不存在或不属于当前项目");
        }
        if (baseMapper.deleteById(id) > 0) {
            return ResponseEntity.success(true);
        }
        return ResponseEntity.fail("删除客户失败");
    }

    public ResponseEntity<Map<String, Object>> statistics(Long projectId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> statusCounts = baseMapper.countByStatus(projectId);
        List<Map<String, Object>> sourceCounts = baseMapper.countBySource(projectId);
        Long total = lambdaQuery().eq(CustomerMain::getProjectId, projectId).eq(CustomerMain::getDelFlag, 0).count();
        Long todayAdded = baseMapper.countTodayAdded(projectId);
        result.put("total", total);
        result.put("todayAdded", todayAdded);
        result.put("statusCounts", statusCounts);
        result.put("sourceCounts", sourceCounts);
        return ResponseEntity.success(result);
    }
}