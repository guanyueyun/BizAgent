
package com.example.bizagent.modules.aiengine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModuleDesign {

    private String moduleName;
    private String moduleCode;
    private String description;
    private Long projectId;
    private List<TableSchema> tables;
    private List<PageSchema> pages;
    private List<ApiSchema> apis;
    private List<PermissionSchema> permissions;
    private List<MenuSchema> menus;
}
