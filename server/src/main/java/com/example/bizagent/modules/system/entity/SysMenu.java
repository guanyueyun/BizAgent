
package com.example.bizagent.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.bizagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private String menuName;
    private String path;
    private String component;
    private Long parentId;
    private String icon;
    private Integer sortOrder;
    private Integer menuType;
    private String permission;
    private Integer visible;
    private Integer status;
}
