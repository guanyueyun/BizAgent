package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerationRevisionRequest {

    private String instruction;
    private String originalRequirement;
    private ModuleDesign design;
    private String sqlScript;
    private String frontendCode;
    private String backendCode;
    private Long projectId;
    private Long modelConfigId;
    private List<String> agentAssistants;
    private List<String> toolAssistants;
}
