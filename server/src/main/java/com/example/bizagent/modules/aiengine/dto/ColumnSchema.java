
package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

@Data
public class ColumnSchema {

    private String columnName;
    private String dataType;
    private String comment;
    private Boolean nullable;
    private String defaultValue;
    private Boolean primaryKey;
    private Boolean autoIncrement;
}
