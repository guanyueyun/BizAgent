package com.example.bizagent.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bizagent.modules.system.entity.SysModelConfig;
import com.example.bizagent.modules.system.mapper.SysModelConfigMapper;
import com.example.bizagent.modules.system.service.SysModelConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class SysModelConfigServiceImpl extends ServiceImpl<SysModelConfigMapper, SysModelConfig> implements SysModelConfigService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(SysModelConfig config) {
        fillDefaults(config);
        if (Integer.valueOf(1).equals(config.getDefaultFlag())) {
            clearDefault(null);
        }
        save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(SysModelConfig config) {
        fillDefaults(config);
        SysModelConfig existing = getById(config.getId());
        if (existing != null && !StringUtils.hasText(config.getApiKey())) {
            config.setApiKey(existing.getApiKey());
        }
        if (Integer.valueOf(1).equals(config.getDefaultFlag())) {
            clearDefault(config.getId());
        }
        updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        SysModelConfig config = getById(id);
        if (config == null) {
            throw new IllegalArgumentException("模型配置不存在");
        }
        clearDefault(id);
        config.setDefaultFlag(1);
        config.setStatus(1);
        updateById(config);
    }

    @Override
    public SysModelConfig getActiveDefault() {
        SysModelConfig defaultConfig = lambdaQuery()
                .eq(SysModelConfig::getStatus, 1)
                .eq(SysModelConfig::getDefaultFlag, 1)
                .last("LIMIT 1")
                .one();
        if (defaultConfig != null) {
            return defaultConfig;
        }
        return lambdaQuery()
                .eq(SysModelConfig::getStatus, 1)
                .orderByDesc(SysModelConfig::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    private void fillDefaults(SysModelConfig config) {
        if (!StringUtils.hasText(config.getConfigName())) {
            throw new IllegalArgumentException("配置名称不能为空");
        }
        if (!StringUtils.hasText(config.getProvider())) {
            throw new IllegalArgumentException("模型厂商不能为空");
        }
        if (!StringUtils.hasText(config.getModelName())) {
            throw new IllegalArgumentException("模型名称不能为空");
        }
        if (config.getTemperature() == null) {
            config.setTemperature(new BigDecimal("0.70"));
        }
        if (config.getMaxTokens() == null) {
            config.setMaxTokens(4096);
        }
        if (config.getTimeoutSeconds() == null) {
            config.setTimeoutSeconds(60);
        }
        if (config.getDefaultFlag() == null) {
            config.setDefaultFlag(0);
        }
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        if (config.getProjectId() == null) {
            config.setProjectId(1L);
        }
    }

    private void clearDefault(Long exceptId) {
        UpdateWrapper<SysModelConfig> wrapper = new UpdateWrapper<>();
        wrapper.set("default_flag", 0).eq("default_flag", 1);
        if (Objects.nonNull(exceptId)) {
            wrapper.ne("id", exceptId);
        }
        update(wrapper);
    }
}
