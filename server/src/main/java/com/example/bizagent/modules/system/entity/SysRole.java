
package com.example.bizagent.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.bizagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String roleName;
    private String roleCode;
    private String description;
    private Integer status;
}
