
package com.example.bizagent.modules.aiengine.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionSchema {

    @JsonAlias({"code"})
    private String permissionCode;
    @JsonAlias({"name"})
    private String permissionName;
    private String moduleName;
}
