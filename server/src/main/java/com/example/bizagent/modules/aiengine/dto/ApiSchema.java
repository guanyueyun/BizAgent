
package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

@Data
public class ApiSchema {

    private String apiName;
    private String method;
    private String path;
    private String description;
}
