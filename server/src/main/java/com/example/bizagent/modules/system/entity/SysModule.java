
package com.example.bizagent.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.bizagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_module")
public class SysModule extends BaseEntity {

    private String moduleName;
    private String moduleCode;
    private String description;
    private String frontPath;
    private String backPath;
    private String designJson;
    private Integer status;
    private Integer lifecycle;
}
