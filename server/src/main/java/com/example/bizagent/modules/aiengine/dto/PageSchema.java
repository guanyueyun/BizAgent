
package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageSchema {

    private String pageName;
    private String pageType;
    private String path;
    private String component;
    private List<FieldSchema> fields;
}
