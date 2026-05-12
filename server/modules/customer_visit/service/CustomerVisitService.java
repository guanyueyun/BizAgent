package com.example.bizagent.modules.customer_visit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.customer_visit.entity.CustomerVisitMain;
import com.example.bizagent.modules.customer_visit.mapper.CustomerVisitMainMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CustomerVisitMainService {

    @Autowired
    private CustomerVisitMainMapper customerVisitMainMapper;

    public PageResponse<CustomerVisitMain> list(Long projectId, String customerName, String visitMethod, String follower, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<CustomerVisitMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerVisitMain::getDelFlag, 0);
        wrapper.eq(CustomerVisitMain::getProjectId, projectId);
        if (StringUtils.hasText(customerName)) {
            wrapper.like(CustomerVisitMain::getCustomerName, customerName);
        }
        if (StringUtils.hasText(visitMethod)) {
            wrapper.eq(CustomerVisitMain::getVisitMethod, visitMethod);
        }
        if (StringUtils.hasText(follower)) {
            wrapper.like(CustomerVisitMain::getFollower, follower);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(CustomerVisitMain::getStatus, status);
        }
        wrapper.orderByDesc(CustomerVisitMain::getCreateTime);
        IPage<CustomerVisitMain> page = customerVisitMainMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResponse.success(page.getRecords(), page.getTotal());
    }

    public ResponseEntity<CustomerVisitMain> detail(Long id, Long projectId) {
        CustomerVisitMain entity = customerVisitMainMapper.selectOne(
                new LambdaQueryWrapper<CustomerVisitMain>()
                        .eq(CustomerVisitMain::getId, id)
                        .eq(CustomerVisitMain::getDelFlag, 0)
                        .eq(CustomerVisitMain::getProjectId, projectId)
        );
        if (entity == null) {
            return ResponseEntity.error("记录不存在");
        }
        return ResponseEntity.success(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<String> create(CustomerVisitMain entity, Long projectId) {
        if (!StringUtils.hasText(entity.getCustomerName())) {
            return ResponseEntity.error("客户名称不能为空");
        }
        if (entity.getVisitTime() == null) {
            return ResponseEntity.error("回访时间不能为空");
        }
        if (!StringUtils.hasText(entity.getVisitMethod())) {
            return ResponseEntity.error("回访方式不能为空");
        }
        if (!StringUtils.hasText(entity.getFollower())) {
            return ResponseEntity.error("跟进人不能为空");
        }
        entity.setProjectId(projectId);
        customerVisitMainMapper.insert(entity);
        return ResponseEntity.success("新增成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<String> update(CustomerVisitMain entity, Long id, Long projectId) {
        CustomerVisitMain existing = customerVisitMainMapper.selectOne(
                new LambdaQueryWrapper<CustomerVisitMain>()
                        .eq(CustomerVisitMain::getId, id)
                        .eq(CustomerVisitMain::getDelFlag, 0)
                        .eq(CustomerVisitMain::getProjectId, projectId)
        );
        if (existing == null) {
            return ResponseEntity.error("记录不存在");
        }
        if (!StringUtils.hasText(entity.getCustomerName())) {
            return ResponseEntity.error("客户名称不能为空");
        }
        if (entity.getVisitTime() == null) {
            return ResponseEntity.error("回访时间不能为空");
        }
        if (!StringUtils.hasText(entity.getVisitMethod())) {
            return ResponseEntity.error("回访方式不能为空");
        }
        if (!StringUtils.hasText(entity.getFollower())) {
            return ResponseEntity.error("跟进人不能为空");
        }
        entity.setId(id);
        entity.setProjectId(projectId);
        customerVisitMainMapper.updateById(entity);
        return ResponseEntity.success("更新成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<String> delete(Long id, Long projectId) {
        CustomerVisitMain existing = customerVisitMainMapper.selectOne(
                new LambdaQueryWrapper<CustomerVisitMain>()
                        .eq(CustomerVisitMain::getId, id)
                        .eq(CustomerVisitMain::getDelFlag, 0)
                        .eq(CustomerVisitMain::getProjectId, projectId)
        );
        if (existing == null) {
            return ResponseEntity.error("记录不存在");
        }
        customerVisitMainMapper.deleteById(id);
        return ResponseEntity.success("删除成功");
    }

    public ResponseEntity<Map<String, Object>> statistics(Long projectId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> methodStats = customerVisitMainMapper.countByVisitMethod(projectId);
        List<Map<String, Object>> statusStats = customerVisitMainMapper.countByStatus(projectId);
        List<Map<String, Object>> monthlyStats = customerVisitMainMapper.countByMonth(projectId);
        result.put("visitMethodStats", methodStats);
        result.put("statusStats", statusStats);
        result.put("monthlyStats", monthlyStats);
        return ResponseEntity.success(result);
    }
}