
package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

@Data
public class RequirementRequest {

    private String requirement;
    private Long projectId;
    private Long modelConfigId;
    private Boolean needApproval;
    private Boolean needMobile;
    private Boolean needImportExport;
    private Boolean needStatistics;
    private Boolean needNotification;
}
