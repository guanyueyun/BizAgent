
package com.example.bizagent.modules.aiengine.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldSchema {

    @JsonAlias({"name", "columnName"})
    private String fieldName;
    private String label;
    private String type;
    private Boolean required;
    private String placeholder;
    private String options;
}
