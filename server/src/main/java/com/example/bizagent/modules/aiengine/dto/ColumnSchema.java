
package com.example.bizagent.modules.aiengine.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColumnSchema {

    @JsonAlias({"name", "fieldName"})
    private String columnName;
    @JsonAlias({"type", "columnType"})
    private String dataType;
    @JsonAlias({"label", "description"})
    private String comment;
    private Boolean nullable;
    private String defaultValue;
    private Boolean primaryKey;
    private Boolean autoIncrement;
}
