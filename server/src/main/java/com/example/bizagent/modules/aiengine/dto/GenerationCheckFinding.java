package com.example.bizagent.modules.aiengine.dto;

import lombok.Data;

@Data
public class GenerationCheckFinding {

    private String severity;
    private String category;
    private String target;
    private String message;

    public static GenerationCheckFinding of(String severity, String category, String target, String message) {
        GenerationCheckFinding finding = new GenerationCheckFinding();
        finding.setSeverity(severity);
        finding.setCategory(category);
        finding.setTarget(target);
        finding.setMessage(message);
        return finding;
    }
}
