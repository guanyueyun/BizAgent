
package com.example.bizagent.modules.aiengine.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TableSchema {

    @JsonAlias({"name"})
    private String tableName;
    @JsonAlias({"comment", "description"})
    private String tableComment;
    private List<ColumnSchema> columns;
}
