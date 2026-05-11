
package com.example.bizagent.modules.aiengine.service;

import com.example.bizagent.modules.aiengine.dto.ModuleDesign;
import com.example.bizagent.modules.aiengine.dto.RequirementRequest;
import com.example.bizagent.modules.system.entity.SysModule;

import java.util.List;

public interface AiEngineService {

    ModuleDesign analyzeRequirement(RequirementRequest request);

    String optimizeRequirement(RequirementRequest request);

    List<String> suggestQuestions(RequirementRequest request);

    String generateFrontendCode(ModuleDesign design);

    String generateBackendCode(ModuleDesign design);

    String generateSqlScript(ModuleDesign design);

    SysModule publishModule(ModuleDesign design);
}
