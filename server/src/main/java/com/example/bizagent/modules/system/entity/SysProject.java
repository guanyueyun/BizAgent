
package com.example.bizagent.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.bizagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_project")
public class SysProject extends BaseEntity {

    private String projectName;
    private String projectCode;
    private String description;
    private Integer status;
}
