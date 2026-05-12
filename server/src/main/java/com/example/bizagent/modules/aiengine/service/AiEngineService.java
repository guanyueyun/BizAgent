
package com.example.bizagent.modules.aiengine.service;

import com.example.bizagent.modules.aiengine.dto.ModuleDesign;
import com.example.bizagent.modules.aiengine.dto.RequirementRequest;
import com.example.bizagent.modules.aiengine.dto.GenerationRevisionRequest;
import com.example.bizagent.modules.aiengine.dto.GenerationCheckRequest;
import com.example.bizagent.modules.aiengine.dto.GenerationCheckResult;
import com.example.bizagent.modules.aiengine.dto.GenerationPlan;
import com.example.bizagent.modules.system.entity.SysModule;

import java.util.List;

public interface AiEngineService {

    ModuleDesign analyzeRequirement(RequirementRequest request);

    GenerationPlan planGeneration(RequirementRequest request);

    String optimizeRequirement(RequirementRequest request);

    List<String> suggestQuestions(RequirementRequest request);

    String generateFrontendCode(ModuleDesign design);

    String generateBackendCode(ModuleDesign design);

    String generateSqlScript(ModuleDesign design);

    ModuleDesign reviseDesign(GenerationRevisionRequest request);

    GenerationCheckResult checkGeneration(GenerationCheckRequest request);

    SysModule publishModule(ModuleDesign design);
}
