package com.example.bizagent.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.bizagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_model_config")
public class SysModelConfig extends BaseEntity {

    private String configName;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer timeoutSeconds;
    private Integer retryCount;
    private Integer defaultFlag;
    private Integer status;
    private String description;
}
