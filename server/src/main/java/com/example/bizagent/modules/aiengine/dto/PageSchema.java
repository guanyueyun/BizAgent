
package com.example.bizagent.modules.aiengine.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageSchema {

    @JsonAlias({"name", "title"})
    private String pageName;
    @JsonAlias({"type"})
    private String pageType;
    private String path;
    private String component;
    private List<FieldSchema> fields;
}
