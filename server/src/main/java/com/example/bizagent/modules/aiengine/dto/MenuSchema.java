
package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

@Data
public class MenuSchema {

    private String menuName;
    private String path;
    private String component;
    private Long parentId;
    private String icon;
    private Integer sortOrder;
    private Integer menuType;
}
