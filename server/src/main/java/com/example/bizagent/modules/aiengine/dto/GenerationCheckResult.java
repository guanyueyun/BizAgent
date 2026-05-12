package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerationCheckResult {

    private boolean passed;
    private List<GenerationCheckFinding> findings;
    private List<String> allowedDirectories;
    private List<String> blockedFiles;
    private List<String> featureFlags;
}
