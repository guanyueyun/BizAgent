
package com.example.bizagent.modules.aiengine.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiSchema {

    @JsonAlias({"name"})
    private String apiName;
    private String method;
    private String path;
    private String description;
}
