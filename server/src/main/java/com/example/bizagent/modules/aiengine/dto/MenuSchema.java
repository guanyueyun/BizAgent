
package com.example.bizagent.modules.aiengine.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MenuSchema {

    @JsonAlias({"name", "title"})
    private String menuName;
    private String path;
    private String component;
    private Long parentId;
    private String icon;
    private Integer sortOrder;
    private Integer menuType;
}
