
package com.example.bizagent.modules.aiengine.controller;

import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.common.OperationLogService;
import com.example.bizagent.common.auth.CurrentUser;
import com.example.bizagent.modules.aiengine.dto.ModuleDesign;
import com.example.bizagent.modules.aiengine.dto.RequirementRequest;
import com.example.bizagent.modules.aiengine.service.AiEngineService;
import com.example.bizagent.modules.modulecontainer.service.ModuleLoaderService;
import com.example.bizagent.modules.system.entity.SysModule;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiEngineController {

    private final AiEngineService aiEngineService;
    private final ModuleLoaderService moduleLoaderService;
    private final OperationLogService operationLogService;

    public AiEngineController(AiEngineService aiEngineService,
                              ModuleLoaderService moduleLoaderService,
                              OperationLogService operationLogService) {
        this.aiEngineService = aiEngineService;
        this.moduleLoaderService = moduleLoaderService;
        this.operationLogService = operationLogService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ModuleDesign> analyzeRequirement(@RequestBody RequirementRequest request) {
        CurrentUser.requirePermission("ai:generate");
        ModuleDesign design = aiEngineService.analyzeRequirement(request);
        operationLogService.log("AI_ANALYZE", design.getModuleCode(), design.getProjectId(), "SUCCESS", "需求分析完成");
        return ResponseEntity.success("需求分析完成", design);
    }

    @PostMapping("/optimize")
    public ResponseEntity<String> optimizeRequirement(@RequestBody RequirementRequest request) {
        CurrentUser.requirePermission("ai:generate");
        String result = aiEngineService.optimizeRequirement(request);
        operationLogService.log("AI_OPTIMIZE", null, request.getProjectId(), "SUCCESS", "需求优化完成");
        return ResponseEntity.success("需求优化完成", result);
    }

    @PostMapping("/questions")
    public ResponseEntity<List<String>> suggestQuestions(@RequestBody RequirementRequest request) {
        CurrentUser.requirePermission("ai:generate");
        List<String> questions = aiEngineService.suggestQuestions(request);
        operationLogService.log("AI_QUESTIONS", null, request.getProjectId(), "SUCCESS", "需求追问生成完成");
        return ResponseEntity.success("需求追问生成完成", questions);
    }

    @PostMapping("/generate/frontend")
    public ResponseEntity<String> generateFrontend(@RequestBody ModuleDesign design) {
        CurrentUser.requirePermission("ai:generate");
        String code = aiEngineService.generateFrontendCode(design);
        operationLogService.log("AI_GENERATE_FRONTEND", design.getModuleCode(), design.getProjectId(), "SUCCESS", "前端代码生成完成");
        return ResponseEntity.success("前端代码生成完成", code);
    }

    @PostMapping("/generate/backend")
    public ResponseEntity<String> generateBackend(@RequestBody ModuleDesign design) {
        CurrentUser.requirePermission("ai:generate");
        String code = aiEngineService.generateBackendCode(design);
        operationLogService.log("AI_GENERATE_BACKEND", design.getModuleCode(), design.getProjectId(), "SUCCESS", "后端代码生成完成");
        return ResponseEntity.success("后端代码生成完成", code);
    }

    @PostMapping("/generate/sql")
    public ResponseEntity<String> generateSql(@RequestBody ModuleDesign design) {
        CurrentUser.requirePermission("ai:generate");
        String script = aiEngineService.generateSqlScript(design);
        operationLogService.log("AI_GENERATE_SQL", design.getModuleCode(), design.getProjectId(), "SUCCESS", "SQL脚本生成完成");
        return ResponseEntity.success("SQL脚本生成完成", script);
    }

    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewModule(@RequestBody ModuleDesign design) {
        CurrentUser.requirePermission("ai:preview");
        Map<String, Object> preview = new HashMap<>();
        preview.put("design", design);
        preview.put("frontendCode", aiEngineService.generateFrontendCode(design));
        preview.put("backendCode", aiEngineService.generateBackendCode(design));
        preview.put("sqlScript", aiEngineService.generateSqlScript(design));
        operationLogService.log("AI_PREVIEW", design.getModuleCode(), design.getProjectId(), "SUCCESS", "模块预览完成");
        return ResponseEntity.success("预览完成", preview);
    }

    @PostMapping("/publish")
    public ResponseEntity<SysModule> publishModule(@RequestBody ModuleDesign design) {
        CurrentUser.requirePermission("ai:publish");
        SysModule module = aiEngineService.publishModule(design);
        moduleLoaderService.loadModule(module.getId());
        operationLogService.log("AI_DEPLOY", module.getModuleCode(), module.getProjectId(), "SUCCESS", "模块部署并加载完成");
        return ResponseEntity.success("模块发布成功", module);
    }

    @PostMapping("/complete")
    public ResponseEntity<Map<String, Object>> completeGeneration(@RequestBody RequirementRequest request) {
        CurrentUser.requirePermission("ai:publish");
        ModuleDesign design = aiEngineService.analyzeRequirement(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("design", design);
        result.put("frontendCode", aiEngineService.generateFrontendCode(design));
        result.put("backendCode", aiEngineService.generateBackendCode(design));
        result.put("sqlScript", aiEngineService.generateSqlScript(design));
        SysModule module = aiEngineService.publishModule(design);
        moduleLoaderService.loadModule(module.getId());
        operationLogService.log("AI_COMPLETE", module.getModuleCode(), module.getProjectId(), "SUCCESS", "一键生成、发布和加载完成");
        result.put("module", module);
        result.put("published", true);
        result.put("loaded", true);
        result.put("runtimePath", "/module-runtime/" + module.getModuleCode());
        
        return ResponseEntity.success("模块生成并发布完成", result);
    }
}
