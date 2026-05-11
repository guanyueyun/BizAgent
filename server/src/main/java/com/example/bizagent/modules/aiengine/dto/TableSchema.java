
package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class TableSchema {

    private String tableName;
    private String tableComment;
    private List<ColumnSchema> columns;
}
