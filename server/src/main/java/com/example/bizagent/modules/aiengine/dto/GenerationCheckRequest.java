package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

import java.util.Map;

@Data
public class GenerationCheckRequest {

    private ModuleDesign design;
    private String sqlScript;
    private String frontendCode;
    private String backendCode;
    private Map<String, String> generatedFiles;
}
