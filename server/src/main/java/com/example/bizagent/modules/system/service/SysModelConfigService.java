package com.example.bizagent.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.bizagent.modules.system.entity.SysModelConfig;

public interface SysModelConfigService extends IService<SysModelConfig> {

    void saveConfig(SysModelConfig config);

    void updateConfig(SysModelConfig config);

    void setDefault(Long id);

    SysModelConfig getActiveDefault();
}
