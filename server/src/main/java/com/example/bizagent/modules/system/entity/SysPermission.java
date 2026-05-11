
package com.example.bizagent.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.bizagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    private String permissionCode;
    private String permissionName;
    private String moduleName;
    private String description;
}
