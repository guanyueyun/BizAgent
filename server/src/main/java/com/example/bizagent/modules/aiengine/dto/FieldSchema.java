
package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

@Data
public class FieldSchema {

    private String fieldName;
    private String label;
    private String type;
    private Boolean required;
    private String placeholder;
    private String options;
}
