package com.example.bizagent.modules.module.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bizagent.modules.module.entity.ModuleEntity;
import com.example.bizagent.modules.module.mapper.ModuleMapper;
import org.springframework.stereotype.Service;

@Service
public class ModuleService extends ServiceImpl<ModuleMapper, ModuleEntity> {

    public ModuleService(ModuleMapper mapper) {
        super(mapper);
    }
}
