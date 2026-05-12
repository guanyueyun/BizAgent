package com.example.bizagent.modules.contract.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bizagent.modules.contract.entity.ContractEntity;
import com.example.bizagent.modules.contract.mapper.ContractMapper;
import org.springframework.stereotype.Service;

@Service
public class ContractService extends ServiceImpl<ContractMapper, ContractEntity> {

    public ContractService(ContractMapper mapper) {
        super(mapper);
    }
}
