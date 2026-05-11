package com.example.bizagent.modules.approval.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bizagent.modules.approval.entity.ApprovalEntity;
import com.example.bizagent.modules.approval.mapper.ApprovalMapper;
import org.springframework.stereotype.Service;

@Service
public class ApprovalService extends ServiceImpl<ApprovalMapper, ApprovalEntity> {

    public ApprovalService(ApprovalMapper mapper) {
        super(mapper);
    }
}
