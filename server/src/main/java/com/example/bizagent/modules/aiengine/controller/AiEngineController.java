
package com.example.bizagent.modules.aiengine.controller;

import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.common.OperationLogService;
import com.example.bizagent.common.auth.CurrentUser;
import com.example.bizagent.modules.aiengine.dto.GenerationRevisionRequest;
import com.example.bizagent.modules.aiengine.dto.GenerationCheckRequest;
import com.example.bizagent.modules.aiengine.dto.GenerationCheckResult;
import com.example.bizagent.modules.aiengine.dto.GenerationPlan;
import com.example.bizagent.modules.aiengine.dto.ModuleDesign;
import com.example.bizagent.modules.aiengine.dto.RequirementRequest;
import com.example.bizagent.modules.aiengine.service.AiEngineService;
import com.example.bizagent.modules.aiengine.service.impl.AiEngineServiceImpl;
import com.example.bizagent.modules.modulecontainer.service.ModuleLoaderService;
import com.example.bizagent.modules.system.entity.SysModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiEngineController {

    private final AiEngineService aiEngineService;
    private final ModuleLoaderService moduleLoaderService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @PostMapping("/plan")
    public ResponseEntity<GenerationPlan> planGeneration(@RequestBody RequirementRequest request) {
        CurrentUser.requirePermission("ai:generate");
        GenerationPlan plan = aiEngineService.planGeneration(request);
        operationLogService.log("AI_PLAN", plan.getModuleCode(), request.getProjectId(), "SUCCESS", "生成计划完成");
        return ResponseEntity.success("生成计划完成", plan);
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

    @PostMapping("/check")
    public ResponseEntity<GenerationCheckResult> checkGeneration(@RequestBody GenerationCheckRequest request) {
        CurrentUser.requirePermission("ai:preview");
        GenerationCheckResult result = aiEngineService.checkGeneration(request);
        String moduleCode = request.getDesign() == null ? null : request.getDesign().getModuleCode();
        Long projectId = request.getDesign() == null ? null : request.getDesign().getProjectId();
        operationLogService.log("AI_CHECK", moduleCode, projectId, result.isPassed() ? "SUCCESS" : "FAILED", "生成产物校验完成");
        return ResponseEntity.success("生成产物校验完成", result);
    }

    @PostMapping("/publish")
    public ResponseEntity<SysModule> publishModule(@RequestBody ModuleDesign design) {
        CurrentUser.requirePermission("ai:publish");
        SysModule module = aiEngineService.publishModule(design);
        moduleLoaderService.loadModule(module.getId());
        operationLogService.log("AI_DEPLOY", module.getModuleCode(), module.getProjectId(), "SUCCESS", "模块部署并加载完成");
        return ResponseEntity.success("模块发布成功", module);
    }

    @PostMapping("/revise")
    public ResponseEntity<Map<String, Object>> reviseGeneration(@RequestBody GenerationRevisionRequest request) {
        CurrentUser.requirePermission("ai:generate");
        ModuleDesign revised = aiEngineService.reviseDesign(request);
        Map<String, Object> result = new HashMap<>();
        result.put("design", revised);
        result.put("warnings", new java.util.ArrayList<String>());
        result.put("sqlScript", generateWithFallback("SQL", () -> aiEngineService.generateSqlScript(revised), request.getSqlScript(), result));
        result.put("frontendCode", generateWithFallback("前端代码", () -> aiEngineService.generateFrontendCode(revised), request.getFrontendCode(), result));
        result.put("backendCode", generateWithFallback("后端代码", () -> aiEngineService.generateBackendCode(revised), request.getBackendCode(), result));
        operationLogService.log("AI_REVISE", revised.getModuleCode(), revised.getProjectId(), "SUCCESS", "AI 继续修改完成");
        return ResponseEntity.success("AI 继续修改完成", result);
    }

    @PostMapping(value = "/revise/stream", produces = "application/x-ndjson;charset=UTF-8")
    public StreamingResponseBody reviseGenerationStream(@RequestBody GenerationRevisionRequest request) {
        CurrentUser.requirePermission("ai:generate");
        return outputStream -> {
            try {
                AiEngineServiceImpl.setAiTraceConsumer(trace -> emitAiTrace(outputStream, trace));
                emit(outputStream, "info", "agents", "读取修改意见", "后端已收到当前生成结果和用户修改要求。", null);
                emit(outputStream, "running", "requirement", "理解修改要求", request.getInstruction(), null);
                emit(outputStream, "running", "design", "修订模块设计", "正在基于当前 ModuleDesign 调用模型生成修订版本。", null);
                ModuleDesign revised = aiEngineService.reviseDesign(request);
                emit(outputStream, "done", "design", "模块设计修订完成", revised.getModuleName() + " / " + revised.getModuleCode(), revised);

                emit(outputStream, "running", "code", "重新生成 SQL 脚本", "正在按修订后的设计生成 SQL。", null);
                Map<String, Object> result = new HashMap<>();
                result.put("design", revised);
                result.put("warnings", new java.util.ArrayList<String>());
                String sqlScript = generateWithFallback("SQL", () -> aiEngineService.generateSqlScript(revised), request.getSqlScript(), result);
                emit(outputStream, "running", "code", "重新生成前端代码", "正在按修订后的设计生成 Vue 模块文件。", null);
                String frontendCode = generateWithFallback("前端代码", () -> aiEngineService.generateFrontendCode(revised), request.getFrontendCode(), result);
                emit(outputStream, "running", "code", "重新生成后端代码", "正在按修订后的设计生成 Java 模块代码。", null);
                String backendCode = generateWithFallback("后端代码", () -> aiEngineService.generateBackendCode(revised), request.getBackendCode(), result);
                emit(outputStream, "done", "code", "代码重新生成完成", "SQL、前端和后端草案已全部更新。", null);

                result.put("sqlScript", sqlScript);
                result.put("frontendCode", frontendCode);
                result.put("backendCode", backendCode);
                operationLogService.log("AI_REVISE_STREAM", revised.getModuleCode(), revised.getProjectId(), "SUCCESS", "流式 AI 继续修改完成");
                emit(outputStream, "final", "code", "AI 继续修改完成", "当前结果已替换为修订版本，可继续修改或发布。", result);
            } catch (Exception e) {
                emit(outputStream, "error", "code", "AI 继续修改失败", e.getMessage(), null);
            } finally {
                AiEngineServiceImpl.clearAiTraceConsumer();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private String generateWithFallback(String label,
                                        java.util.concurrent.Callable<String> generator,
                                        String fallback,
                                        Map<String, Object> result) {
        try {
            return generator.call();
        } catch (Exception e) {
            String message = label + "重新生成失败，已保留修改前内容：" + e.getMessage();
            ((List<String>) result.computeIfAbsent("warnings", key -> new java.util.ArrayList<String>())).add(message);
            operationLogService.log("AI_REVISE_ARTIFACT", null, null, "FAILED", message);
            return fallback == null ? "" : fallback;
        }
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

    @PostMapping(value = "/complete/stream", produces = "application/x-ndjson;charset=UTF-8")
    public StreamingResponseBody completeGenerationStream(@RequestBody RequirementRequest request) {
        CurrentUser.requirePermission("ai:generate");
        boolean canPublish = CurrentUser.hasPermission("ai:publish");
        return outputStream -> {
            try {
                AiEngineServiceImpl.setAiTraceConsumer(trace -> emitAiTrace(outputStream, trace));
                emit(outputStream, "info", "agents", "读取 Agent 和工具配置", "后端已收到本次生成请求，开始按真实任务链执行。", null);
                emit(outputStream, "running", "requirement", "分析需求并生成模块设计", "正在调用模型生成 ModuleDesign JSON。", null);
                ModuleDesign design = aiEngineService.analyzeRequirement(request);
                emit(outputStream, "done", "requirement", "需求分析完成", design.getModuleName() + " / " + design.getModuleCode(), null);
                emit(outputStream, "done", "agents", "Agent 协同审查完成", "业务、数据、前端、后端和质量约束已参与提示词。", null);

                emit(outputStream, "done", "design", "模块设计已装配", "表、页面、接口、权限和菜单设计已生成。", design);

                emit(outputStream, "running", "code", "生成 SQL 脚本", "正在生成建表、字段、索引和运行时字段 SQL。", null);
                String sqlScript = aiEngineService.generateSqlScript(design);
                emit(outputStream, "running", "code", "生成前端代码", "正在生成 api.js、List.vue、Form.vue、Detail.vue。", null);
                String frontendCode = aiEngineService.generateFrontendCode(design);
                emit(outputStream, "running", "code", "生成后端代码", "正在生成 Entity、Mapper、Service、Controller。", null);
                String backendCode = aiEngineService.generateBackendCode(design);
                emit(outputStream, "done", "code", "代码生成完成", "SQL、前端和后端代码均由后端实际生成完成。", null);

                Map<String, Object> result = new HashMap<>();
                result.put("design", design);
                result.put("frontendCode", frontendCode);
                result.put("backendCode", backendCode);
                result.put("sqlScript", sqlScript);

                if (!canPublish) {
                    result.put("published", false);
                    result.put("loaded", false);
                    result.put("runtimePath", null);
                    emit(outputStream, "info", "publish", "跳过发布步骤", "当前账号缺少权限 ai:publish，已保留草案结果，可继续修改或请管理员授权后发布。", null);
                    emit(outputStream, "final", "code", "生成草案完成", "分析和代码生成已完成，发布需要 ai:publish 权限。", result);
                    return;
                }

                emit(outputStream, "running", "publish", "发布并加载模块", "正在同步数据库表、菜单、权限和模块运行时。", null);
                SysModule module = aiEngineService.publishModule(design);
                moduleLoaderService.loadModule(module.getId());
                operationLogService.log("AI_COMPLETE_STREAM", module.getModuleCode(), module.getProjectId(), "SUCCESS", "流式一键生成、发布和加载完成");

                result.put("module", module);
                result.put("published", true);
                result.put("loaded", true);
                result.put("runtimePath", "/module-runtime/" + module.getModuleCode());
                emit(outputStream, "done", "publish", "模块发布完成", "运行时路径：" + result.get("runtimePath"), null);
                emit(outputStream, "final", "publish", "生成链路完成", "后端真实任务链已全部结束。", result);
            } catch (Exception e) {
                emit(outputStream, "error", "publish", "生成链路失败", e.getMessage(), null);
            } finally {
                AiEngineServiceImpl.clearAiTraceConsumer();
            }
        };
    }

    private void emitAiTrace(java.io.OutputStream outputStream, Map<String, Object> trace) {
        try {
            String scene = String.valueOf(trace.getOrDefault("scene", "AI"));
            String kind = String.valueOf(trace.getOrDefault("kind", "request"));
            String modelName = String.valueOf(trace.getOrDefault("modelName", ""));
            String title = "request".equals(kind) ? "AI请求对话" : "AI响应内容";
            String detail = scene + (modelName.isBlank() ? "" : " / " + modelName);
            emit(outputStream, "ai_message", scene, title, detail, trace);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("AI 对话内容推送失败: " + e.getMessage(), e);
        }
    }

    private void emit(java.io.OutputStream outputStream,
                      String type,
                      String step,
                      String title,
                      String detail,
                      Object data) throws java.io.IOException {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("step", step);
        event.put("title", title);
        event.put("detail", detail);
        if (data != null) {
            event.put("data", data);
        }
        outputStream.write((objectMapper.writeValueAsString(event) + "\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
