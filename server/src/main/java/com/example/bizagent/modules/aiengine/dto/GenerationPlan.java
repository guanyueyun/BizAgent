package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerationPlan {

    private String moduleName;
    private String moduleCode;
    private String requirementSummary;
    private List<String> pages;
    private List<String> apis;
    private List<String> tables;
    private List<String> permissions;
    private List<String> risks;
    private List<String> plannedFiles;
    private List<String> skills;
    private List<String> featureFlags;
    private ModuleDesign design;
}
