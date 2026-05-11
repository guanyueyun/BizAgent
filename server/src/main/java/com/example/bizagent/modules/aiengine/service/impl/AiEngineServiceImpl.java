package com.example.bizagent.modules.aiengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bizagent.common.OperationLogService;
import com.example.bizagent.modules.aiengine.dto.*;
import com.example.bizagent.modules.aiengine.service.AiEngineService;
import com.example.bizagent.modules.system.entity.SysMenu;
import com.example.bizagent.modules.system.entity.SysModelConfig;
import com.example.bizagent.modules.system.entity.SysModule;
import com.example.bizagent.modules.system.entity.SysPermission;
import com.example.bizagent.modules.system.service.SysMenuService;
import com.example.bizagent.modules.system.service.SysModelConfigService;
import com.example.bizagent.modules.system.service.SysModuleService;
import com.example.bizagent.modules.system.service.SysPermissionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AiEngineServiceImpl implements AiEngineService {

    private final SysModuleService sysModuleService;
    private final SysMenuService sysMenuService;
    private final SysPermissionService sysPermissionService;
    private final SysModelConfigService sysModelConfigService;
    private final OperationLogService operationLogService;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiEngineServiceImpl(SysModuleService sysModuleService,
                               SysMenuService sysMenuService,
                               SysPermissionService sysPermissionService,
                               SysModelConfigService sysModelConfigService,
                               OperationLogService operationLogService,
                               DataSource dataSource) {
        this.sysModuleService = sysModuleService;
        this.sysMenuService = sysMenuService;
        this.sysPermissionService = sysPermissionService;
        this.sysModelConfigService = sysModelConfigService;
        this.operationLogService = operationLogService;
        this.dataSource = dataSource;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public ModuleDesign analyzeRequirement(RequirementRequest request) {
        String requirement = Objects.toString(request.getRequirement(), "").trim();
        if (!StringUtils.hasText(requirement)) {
            throw new IllegalArgumentException("需求描述不能为空");
        }
        ModuleDesign aiDesign = analyzeWithConfiguredModel(request);
        if (aiDesign != null) {
            normalizeAiDesign(aiDesign, request);
            return aiDesign;
        }
        throw new IllegalStateException("AI 需求分析失败，请检查模型配置、API Key、模型接口和返回 JSON 格式");
    }

    @Override
    public String optimizeRequirement(RequirementRequest request) {
        String requirement = Objects.toString(request.getRequirement(), "").trim();
        if (!StringUtils.hasText(requirement)) {
            throw new IllegalArgumentException("需求描述不能为空");
        }
        String aiResult = optimizeRequirementWithConfiguredModel(request);
        if (StringUtils.hasText(aiResult)) {
            return aiResult;
        }
        throw new IllegalStateException("AI 需求优化失败，请检查模型配置、API Key 和模型接口");
    }

    private String optimizeRequirementWithConfiguredModel(RequirementRequest request) {
        SysModelConfig config = request.getModelConfigId() != null
                ? sysModelConfigService.getById(request.getModelConfigId())
                : sysModelConfigService.getActiveDefault();
        if (config == null || !StringUtils.hasText(config.getApiKey()) || "******".equals(config.getApiKey())) {
            return "";
        }
        String baseUrl = StringUtils.hasText(config.getBaseUrl()) ? config.getBaseUrl() : "https://api.openai.com/v1";
        String endpoint = baseUrl.replaceAll("/+$", "") + "/chat/completions";
        try {
            Map<String, Object> payload = Map.of(
                    "model", config.getModelName(),
                    "temperature", config.getTemperature() == null ? 0.3 : config.getTemperature(),
                    "max_tokens", Math.min(config.getMaxTokens() == null ? 2048 : config.getMaxTokens(), 4096),
                    "messages", List.of(
                            Map.of("role", "system", "content", requirementOptimizePrompt()),
                            Map.of("role", "user", "content", objectMapper.writeValueAsString(request))
                    )
            );
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds() == null ? 60 : config.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                operationLogService.log("AI_OPTIMIZE_MODEL", null, resolveProjectId(request.getProjectId()), "FAILED", "模型接口返回状态码: " + response.statusCode());
                return "";
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText("").trim();
        } catch (Exception e) {
            operationLogService.log("AI_OPTIMIZE_MODEL", null, resolveProjectId(request.getProjectId()), "FAILED", e.getMessage());
            return "";
        }
    }

    private String requirementOptimizePrompt() {
        return """
                你是 BizAgent 企业低代码平台的需求分析师，目标是把用户口语化需求整理成可直接生成业务功能模块的需求说明。
                必须只输出优化后的中文需求文本，不要 Markdown，不要代码块，不要解释。

                优化目标：
                1. 不改变用户原意，不凭空增加外部系统、登录体系、权限体系、消息中间件、工作流引擎等平台外能力。
                2. 把需求整理为“业务目标、业务对象、核心流程、字段规则、页面、接口、权限、统计、审批、导入导出、消息提醒、移动端适配、数据范围”。
                3. 用户没有说明的信息，用“待确认：...”列出；用户已经勾选的能力必须写入需求。
                4. 保持业务语言清晰具体，避免抽象词，例如“完善管理能力”“提升效率”必须落到对象、字段、动作和状态。

                推荐输出结构：
                业务目标：...
                业务对象：...
                核心流程：...
                字段规则：字段名、类型、是否必填、枚举值、默认值。
                页面要求：列表、新增、编辑、详情，按需包含审批、统计、导入导出、移动端。
                接口要求：查询、新增、修改、删除、详情，按需包含提交、审批、导入、导出、统计、提醒。
                权限要求：模块:list/add/edit/delete，按需包含 approve/import/export/statistics/notify。
                待确认：...
                """;
    }

    private ModuleDesign analyzeWithConfiguredModel(RequirementRequest request) {
        SysModelConfig config = request.getModelConfigId() != null
                ? sysModelConfigService.getById(request.getModelConfigId())
                : sysModelConfigService.getActiveDefault();
        if (config == null || !StringUtils.hasText(config.getApiKey()) || "******".equals(config.getApiKey())) {
            return null;
        }
        String baseUrl = StringUtils.hasText(config.getBaseUrl()) ? config.getBaseUrl() : "https://api.openai.com/v1";
        String endpoint = baseUrl.replaceAll("/+$", "") + "/chat/completions";
        try {
            Map<String, Object> payload = Map.of(
                    "model", config.getModelName(),
                    "temperature", config.getTemperature() == null ? 0.2 : config.getTemperature(),
                    "max_tokens", config.getMaxTokens() == null ? 4096 : config.getMaxTokens(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", userPrompt(request))
                    )
            );
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds() == null ? 60 : config.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                operationLogService.log("AI_ANALYZE_MODEL", null, resolveProjectId(request.getProjectId()), "FAILED", "模型接口返回状态码: " + response.statusCode());
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            String json = extractJsonObject(content);
            if (!StringUtils.hasText(json)) {
                operationLogService.log("AI_ANALYZE_MODEL", null, resolveProjectId(request.getProjectId()), "FAILED", "模型未返回可解析的模块设计 JSON");
                return null;
            }
            return objectMapper.readValue(json, ModuleDesign.class);
        } catch (Exception e) {
            operationLogService.log("AI_ANALYZE_MODEL", null, resolveProjectId(request.getProjectId()), "FAILED", e.getMessage());
            return null;
        }
    }

    private String systemPrompt() {
        return """
                你是 BizAgent 企业低代码平台的业务模块设计生成引擎。
                你的任务是根据用户需求生成一个可被平台发布运行的 ModuleDesign JSON。
                输出必须是严格 JSON，不要 Markdown，不要解释。

                一、绝对边界
                1. 只能生成当前平台内部业务功能模块。
                2. 禁止生成完整系统、登录系统、权限系统、用户中心、组织架构、网关、微服务、独立前端项目、独立后端项目、外部 SaaS。
                3. 禁止要求改造平台基础架构；模块必须挂载到当前 BizAgent 平台。

                二、JSON 契约
                顶层字段必须完整包含：
                moduleName, moduleCode, description, tables, pages, apis, permissions, menus。
                不要输出 null；数组不能为空；不要输出多余解释文本。

                三、命名规范
                1. moduleCode 使用小写英文、数字、下划线，必须以英文字母开头，长度 2-40。
                2. 表名必须使用 biz_{moduleCode}_ 前缀，主表必须是 biz_{moduleCode}_main。
                3. 字段名使用 snake_case 小写英文，不使用中文、拼音首字母混写、空格、连字符。
                4. 权限编码必须为 {moduleCode}:{action}，例如 inspection:list。

                四、表结构规范
                1. 每个模块必须至少有一张主业务表 biz_{moduleCode}_main。
                2. 每张表 columns 中必须包含 id 字段，类型 BIGINT，primaryKey=true，autoIncrement=true。
                3. 不要在 columns 中生成 create_by/create_time/update_by/update_time/del_flag/project_id，平台会统一补充。
                4. 字段类型只能使用 BIGINT、INT、DECIMAL(18,2)、VARCHAR(n)、TEXT、DATE、DATETIME。
                5. 业务字段必须具体，例如 device_name、inspection_time、exception_desc，不要只给 name、remark 两个泛字段。
                6. 状态字段建议使用 status，defaultValue 使用带单引号的枚举值，例如 'draft'。

                五、页面规范
                必须至少包含四个页面：
                list: /{moduleCode}/list
                add: /{moduleCode}/add
                edit: /{moduleCode}/edit/:id
                detail: /{moduleCode}/detail/:id
                每个 page 必须包含 fields，fields 来自主业务表业务字段。
                field.type 只能使用 input、textarea、select、date、datetime、number。
                select 字段必须提供 options，格式为 value:label,value:label。

                六、API 规范
                必须至少包含：
                GET /api/biz/{moduleCode}/list
                GET /api/biz/{moduleCode}/{id}
                POST /api/biz/{moduleCode}
                PUT /api/biz/{moduleCode}/{id}
                DELETE /api/biz/{moduleCode}/{id}
                如果需求包含导入导出，增加 /import、/export。
                如果需求包含统计，增加 /statistics。
                如果需求包含审批，增加 /{id}/submit、/{id}/approve。
                如果需求包含消息提醒，增加 /{id}/notify、/notifications。

                七、权限和菜单规范
                必须至少包含 list、add、edit、delete 权限。
                按需包含 import、export、statistics、approve、notify。
                菜单必须包含一个主菜单：path=/module-runtime/{moduleCode}, component=ModuleRuntime, parentId=0, menuType=2。

                八、常见业务场景参考
                设备巡检：设备、位置、计划、巡检人、巡检时间、异常、状态。
                工单管理：工单号、标题、类型、优先级、处理人、截止时间、处理结果、状态。
                合同管理：合同编号、客户、金额、签订日期、生效日期、到期日期、负责人、状态。
                库存管理：物料编码、物料名称、仓库、数量、单位、供应商、入库时间、状态。
                客户管理：客户名称、联系人、电话、行业、来源、跟进人、跟进状态。
                请假管理：员工、假期类型、开始时间、结束时间、天数、原因、审批状态。

                九、质量要求
                1. 需求越简单，也要生成完整可运行模块设计。
                2. 不要遗漏 pages/apis/permissions/menus 的联动。
                3. 不要输出 Markdown 代码块。
                """;
    }

    private String userPrompt(RequirementRequest request) throws Exception {
        return """
                请根据以下 RequirementRequest 生成平台业务模块设计 JSON。
                needApproval/needMobile/needImportExport/needStatistics/needNotification 为 true 时，必须在 tables、pages、apis、permissions 中体现对应能力。
                输出必须能被 Java ObjectMapper 直接解析为 ModuleDesign。
                RequirementRequest JSON：
                """ + objectMapper.writeValueAsString(request);
    }

    private String extractJsonObject(String content) {
        String text = Objects.toString(content, "").trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return "";
    }

    private void normalizeAiDesign(ModuleDesign design, RequirementRequest request) {
        if (!StringUtils.hasText(design.getModuleCode())) {
            throw new IllegalArgumentException("AI 生成的模块设计缺少 moduleCode");
        }
        if (!StringUtils.hasText(design.getModuleName())) {
            throw new IllegalArgumentException("AI 生成的模块设计缺少 moduleName");
        }
        design.setModuleCode(sanitizeModuleCode(design.getModuleCode()));
        if (!StringUtils.hasText(design.getDescription())) {
            design.setDescription(request.getRequirement());
        }
        design.setProjectId(resolveProjectId(request.getProjectId()));
        design.setModelConfigId(request.getModelConfigId());
        if (design.getTables() == null || design.getTables().isEmpty()) {
            throw new IllegalArgumentException("AI 生成的模块设计缺少 tables");
        }
        if (design.getPages() == null || design.getPages().isEmpty()) {
            throw new IllegalArgumentException("AI 生成的模块设计缺少 pages");
        }
        if (design.getApis() == null || design.getApis().isEmpty()) {
            throw new IllegalArgumentException("AI 生成的模块设计缺少 apis");
        }
        if (design.getPermissions() == null || design.getPermissions().isEmpty()) {
            throw new IllegalArgumentException("AI 生成的模块设计缺少 permissions");
        }
        if (design.getMenus() == null || design.getMenus().isEmpty()) {
            throw new IllegalArgumentException("AI 生成的模块设计缺少 menus");
        }
        normalizeGeneratedDesign(design);
    }

    private void mergeApis(List<ApiSchema> target, List<ApiSchema> required) {
        for (ApiSchema api : required) {
            boolean exists = target.stream().anyMatch(item ->
                    Objects.equals(item.getMethod(), api.getMethod()) && Objects.equals(item.getPath(), api.getPath()));
            if (!exists) {
                target.add(api);
            }
        }
    }

    private void mergeTables(List<TableSchema> target, List<TableSchema> required) {
        for (TableSchema table : required) {
            boolean exists = target.stream().anyMatch(item -> Objects.equals(item.getTableName(), table.getTableName()));
            if (!exists) {
                target.add(table);
            }
        }
    }

    private void mergePermissions(List<PermissionSchema> target, List<PermissionSchema> required) {
        for (PermissionSchema permission : required) {
            boolean exists = target.stream().anyMatch(item -> Objects.equals(item.getPermissionCode(), permission.getPermissionCode()));
            if (!exists) {
                target.add(permission);
            }
        }
    }

    @Override
    public List<String> suggestQuestions(RequirementRequest request) {
        String requirement = Objects.toString(request.getRequirement(), "");
        List<String> questions = new ArrayList<>();
        if (!Boolean.TRUE.equals(request.getNeedApproval()) && !requirement.contains("审批")) {
            questions.add("这个模块是否需要审批流程？如果需要，请说明审批节点和审批人规则。");
        }
        if (!Boolean.TRUE.equals(request.getNeedImportExport()) && !requirement.contains("导入") && !requirement.contains("导出")) {
            questions.add("是否需要 Excel 导入导出？需要导入哪些字段，导出是否要按权限过滤？");
        }
        if (!Boolean.TRUE.equals(request.getNeedStatistics()) && !requirement.contains("统计")) {
            questions.add("是否需要统计看板？请说明统计维度，例如状态、负责人、日期或部门。");
        }
        if (!requirement.contains("字段")) {
            questions.add("核心业务字段有哪些？建议列出名称、类型、是否必填和枚举值。");
        }
        if (!requirement.contains("权限")) {
            questions.add("哪些角色可以查看、新增、编辑、删除、审批或导出这个模块的数据？");
        }
        return questions.stream().limit(5).toList();
    }

    private ColumnSchema column(String name, String type, String comment, boolean nullable, String defaultValue) {
        ColumnSchema column = new ColumnSchema();
        column.setColumnName(name);
        column.setDataType(type);
        column.setComment(comment);
        column.setNullable(nullable);
        column.setDefaultValue(defaultValue);
        column.setPrimaryKey(false);
        column.setAutoIncrement(false);
        return column;
    }

    @Override
    public String generateFrontendCode(ModuleDesign design) {
        validateDesign(design);
        String aiCode = generateByAi(design, "FRONTEND", frontendCodePrompt(), designPrompt(design));
        if (StringUtils.hasText(aiCode)) {
            return aiCode;
        }
        throw new IllegalStateException("AI 前端代码生成失败，请检查模型配置和模型输出");
    }

    @Override
    public String generateBackendCode(ModuleDesign design) {
        validateDesign(design);
        String aiCode = generateByAi(design, "BACKEND", backendCodePrompt(), designPrompt(design));
        if (StringUtils.hasText(aiCode)) {
            return aiCode;
        }
        throw new IllegalStateException("AI 后端代码生成失败，请检查模型配置和模型输出");
    }

    @Override
    public String generateSqlScript(ModuleDesign design) {
        validateDesign(design);
        String aiSql = generateByAi(design, "SQL", sqlCodePrompt(), designPrompt(design));
        if (StringUtils.hasText(aiSql)) {
            return ensureSqlScriptTerminated(aiSql);
        }
        throw new IllegalStateException("AI SQL 生成失败，请检查模型配置和模型输出");
    }

    private String generateByAi(ModuleDesign design, String scene, String systemPrompt, String userPrompt) {
        SysModelConfig config = design.getModelConfigId() != null
                ? sysModelConfigService.getById(design.getModelConfigId())
                : sysModelConfigService.getActiveDefault();
        if (config == null || !StringUtils.hasText(config.getApiKey()) || "******".equals(config.getApiKey())) {
            return "";
        }
        String baseUrl = StringUtils.hasText(config.getBaseUrl()) ? config.getBaseUrl() : "https://api.openai.com/v1";
        String endpoint = baseUrl.replaceAll("/+$", "") + "/chat/completions";
        try {
            Map<String, Object> payload = Map.of(
                    "model", config.getModelName(),
                    "temperature", config.getTemperature() == null ? 0.2 : config.getTemperature(),
                    "max_tokens", Math.min(config.getMaxTokens() == null ? 8192 : config.getMaxTokens(), 12000),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds() == null ? 90 : config.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                operationLogService.log("AI_GENERATE_" + scene, design.getModuleCode(), resolveProjectId(design.getProjectId()), "FAILED", "模型接口返回状态码: " + response.statusCode());
                return "";
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
            return stripCodeFence(content).trim();
        } catch (Exception e) {
            operationLogService.log("AI_GENERATE_" + scene, design.getModuleCode(), resolveProjectId(design.getProjectId()), "FAILED", e.getMessage());
            return "";
        }
    }

    private String designPrompt(ModuleDesign design) {
        try {
            return """
                    请基于以下 ModuleDesign 生成代码。
                    必须严格使用 moduleCode、moduleName、tables、pages、apis、permissions、menus 中的信息。
                    不要新增登录、权限系统、平台架构或外部依赖。
                    ModuleDesign JSON：
                    """ + objectMapper.writeValueAsString(design);
        } catch (Exception e) {
            return "请基于模块编码 " + design.getModuleCode() + " 和模块名称 " + design.getModuleName() + " 生成代码。";
        }
    }

    private String frontendCodePrompt() {
        return """
                你是 BizAgent 平台的 Vue3 + Element Plus 前端代码生成器。
                必须基于输入的 ModuleDesign 生成真实可用的模块前端代码，用于预览和发布后的模块页面。
                只输出代码文本，不要 Markdown，不要解释。

                一、输出格式
                必须严格按以下分段输出，分段标题一字不差：
                // ========== api.js ==========
                // ========== List.vue ==========
                // ========== Form.vue ==========
                // ========== Detail.vue ==========
                如果 ModuleDesign 包含 statistics/approval/mobile 页面，也继续输出：
                // ========== Statistics.vue ==========
                // ========== Approval.vue ==========
                // ========== Mobile.vue ==========

                二、运行约束
                1. Vue 文件使用 Vue3 <script setup>。
                2. UI 使用 Element Plus 组件，不引入未声明的新 UI 库。
                3. api.js 必须从 ../../api 导入 request。
                4. 接口路径必须使用 /biz/{moduleCode}，因为 axios baseURL 已是 /api。
                5. 所有列表、新增、编辑、删除、详情、导入、导出、统计、审批、提醒动作必须调用 api.js。
                6. 不生成 Router、main.js、App.vue、登录、权限系统或独立项目。

                三、页面行为
                List.vue：
                1. 展示主业务表字段，排除 id/create_by/create_time/update_by/update_time/del_flag/project_id，必要时可显示创建时间。
                2. 包含查询区、表格、分页、新增、编辑、详情、删除按钮。
                3. 如果存在 import/export/statistics/approve/notify 权限或 API，页面上提供对应操作入口。
                4. 删除必须二次确认。

                Form.vue：
                1. 支持新增和编辑。
                2. 根据字段类型生成 input/textarea/select/date/datetime/number。
                3. required=true 的字段必须有表单校验。
                4. status/select 字段必须使用 options 显示中文标签。

                Detail.vue：
                1. 使用 el-descriptions 展示所有业务字段和状态中文含义。
                2. 提供返回和编辑事件。

                四、代码质量
                1. 所有变量名使用清晰英文。
                2. 异步操作必须 try/catch，并用 ElMessage 提示成功/失败。
                3. 不要留空函数，不要生成伪代码。
                4. 输出必须是可保存到对应文件的完整代码。
                """;
    }

    private String backendCodePrompt() {
        return """
                你是 BizAgent 平台的 Spring Boot 3 + MyBatis Plus 后端代码生成器。
                必须基于输入的 ModuleDesign 生成模块后端代码草案，供保存、审阅和后续编译集成。
                只输出代码文本，不要 Markdown，不要解释。

                一、输出格式
                必须严格按以下分段输出，分段标题一字不差：
                // ========== Entity ==========
                // ========== Mapper ==========
                // ========== Service ==========
                // ========== Controller ==========

                二、技术约束
                1. package 使用 com.example.bizagent.modules.{moduleCode}.*
                2. 使用 Lombok @Data、MyBatis Plus @TableName、@TableId。
                3. Entity 字段使用 Java 驼峰命名，映射数据库 snake_case 字段。
                4. Controller 路径使用 /api/biz/{moduleCode}。
                5. 返回值使用 com.example.bizagent.common.ResponseEntity 和 PageResponse。
                6. 不生成登录、权限系统、独立项目、pom.xml、application.yml。

                三、接口要求
                1. 必须提供 list/detail/create/update/delete。
                2. list 必须支持 pageNum、pageSize，并过滤 delFlag=0、projectId。
                3. create 必须写入 projectId；update/delete 必须校验 projectId。
                4. delete 使用软删除 delFlag=1。
                5. 如果 ModuleDesign 包含审批、导入导出、统计、消息提醒 API，要补对应方法草案。

                四、代码质量
                1. 输出完整 Java 类代码，不能省略 import。
                2. 不要留 TODO 作为核心逻辑。
                3. 不要使用不存在的平台类。
                """;
    }

    private String sqlCodePrompt() {
        return """
                你是 BizAgent 平台的 MySQL 8 SQL 生成器。
                必须基于输入的 ModuleDesign 生成可执行 SQL，不要复述需求。
                只输出 SQL，不要 Markdown，不要解释。

                一、SQL 范围
                1. 每张表使用 CREATE TABLE IF NOT EXISTS。
                2. 表名必须严格来自 ModuleDesign，不得新增系统表。
                3. 每张业务表必须包含 id BIGINT AUTO_INCREMENT PRIMARY KEY。
                4. 每张业务表必须包含 create_by、create_time、update_by、update_time、del_flag、project_id。
                5. 使用 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4，中文 COMMENT 必须保留。
                6. 禁止 DROP、TRUNCATE、DELETE、UPDATE、ALTER 系统表。

                二、字段要求
                1. 字段顺序：id、业务字段、create_by、create_time、update_by、update_time、del_flag、project_id。
                2. VARCHAR 必须指定长度。
                3. DECIMAL 使用 DECIMAL(18,2)。
                4. 日期使用 DATE，时间使用 DATETIME。
                5. status 默认值必须使用单引号，例如 DEFAULT 'draft'。
                6. create_time 默认 CURRENT_TIMESTAMP，update_time 默认 CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP。

                三、索引建议
                每张主表至少增加普通索引：
                KEY idx_project_id (project_id)
                KEY idx_status (status) 仅当存在 status 字段。
                KEY idx_create_time (create_time)
                """;
    }

    private String stripCodeFence(String content) {
        String text = Objects.toString(content, "").trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z0-9_-]*\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        return text;
    }

    private String ensureSqlScriptTerminated(String sql) {
        String trimmed = sql.trim();
        if (trimmed.isEmpty() || trimmed.endsWith(";")) {
            return trimmed;
        }
        return trimmed + ";";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysModule publishModule(ModuleDesign design) {
        validateDesign(design);
        Long projectId = resolveProjectId(design.getProjectId());
        design.setProjectId(projectId);
        try {
            operationLogService.log("AI_PUBLISH", design.getModuleCode(), projectId, "START", "开始发布 AI 生成模块");
            syncBusinessTables(design);

        SysModule module = findModule(design.getModuleCode(), projectId);
        if (module == null) {
            module = new SysModule();
        }
        module.setModuleName(design.getModuleName());
        module.setModuleCode(design.getModuleCode());
        module.setDescription(design.getDescription());
        module.setFrontPath("/src/modules/" + design.getModuleCode());
        module.setBackPath("/server/modules/" + design.getModuleCode());
        try {
            module.setDesignJson(objectMapper.writeValueAsString(design));
        } catch (Exception e) {
            throw new IllegalStateException("序列化模块设计失败: " + e.getMessage(), e);
        }
        module.setStatus(1);
        module.setLifecycle(2);
        module.setProjectId(projectId);
        if (module.getId() == null) {
            sysModuleService.save(module);
        } else {
            sysModuleService.updateById(module);
        }

        for (PermissionSchema permission : design.getPermissions()) {
            upsertPermission(permission, projectId);
        }
        for (MenuSchema menu : design.getMenus()) {
            upsertMenu(menu, projectId);
        }
        writeModulePackage(design);
        operationLogService.log("AI_PUBLISH", design.getModuleCode(), projectId, "SUCCESS", "模块发布成功");
        return module;
        } catch (RuntimeException e) {
            operationLogService.log("AI_PUBLISH", design.getModuleCode(), projectId, "FAILED", e.getMessage());
            throw e;
        }
    }

    private void upsertPermission(PermissionSchema permission, Long projectId) {
        SysPermission sysPermission = sysPermissionService.getOne(
                new QueryWrapper<SysPermission>()
                        .eq("permission_code", permission.getPermissionCode())
                        .eq("project_id", projectId), false);
        if (sysPermission == null) {
            sysPermission = new SysPermission();
        }
        sysPermission.setPermissionCode(permission.getPermissionCode());
        sysPermission.setPermissionName(permission.getPermissionName());
        sysPermission.setModuleName(permission.getModuleName());
        sysPermission.setProjectId(projectId);
        if (sysPermission.getId() == null) {
            sysPermissionService.save(sysPermission);
        } else {
            sysPermissionService.updateById(sysPermission);
        }
        grantPermissionToAdmin(sysPermission.getId());
    }

    private void grantPermissionToAdmin(Long permissionId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement exists = connection.prepareStatement(
                     "SELECT COUNT(*) FROM sys_role_permission WHERE role_id = 1 AND permission_id = ?");
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO sys_role_permission (role_id, permission_id) VALUES (1, ?)")) {
            exists.setLong(1, permissionId);
            try (ResultSet resultSet = exists.executeQuery()) {
                if (resultSet.next() && resultSet.getLong(1) > 0) {
                    return;
                }
            }
            insert.setLong(1, permissionId);
            insert.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("管理员权限授权失败: " + e.getMessage(), e);
        }
    }

    private void upsertMenu(MenuSchema menu, Long projectId) {
        SysMenu sysMenu = sysMenuService.getOne(new QueryWrapper<SysMenu>()
                .eq("path", menu.getPath())
                .eq("project_id", projectId), false);
        if (sysMenu == null) {
            sysMenu = new SysMenu();
        }
        sysMenu.setMenuName(menu.getMenuName());
        sysMenu.setPath(menu.getPath());
        sysMenu.setComponent(menu.getComponent());
        sysMenu.setParentId(menu.getParentId());
        sysMenu.setIcon(menu.getIcon());
        sysMenu.setSortOrder(menu.getSortOrder());
        sysMenu.setMenuType(menu.getMenuType());
        sysMenu.setPermission(menu.getPath().replace("/module-runtime/", "") + ":list");
        sysMenu.setVisible(1);
        sysMenu.setStatus(1);
        sysMenu.setProjectId(projectId);
        if (sysMenu.getId() == null) {
            sysMenuService.save(sysMenu);
        } else {
            sysMenuService.updateById(sysMenu);
        }
    }

    private SysModule findModule(String moduleCode, Long projectId) {
        return sysModuleService.getOne(new QueryWrapper<SysModule>()
                .eq("module_code", moduleCode)
                .eq("project_id", projectId), false);
    }

    private void syncBusinessTables(ModuleDesign design) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();
            for (TableSchema table : design.getTables()) {
                if (!tableExists(metaData, connection.getCatalog(), table.getTableName())) {
                    statement.execute(createTableSql(table, resolveProjectId(design.getProjectId())));
                    continue;
                }
                for (ColumnSchema column : table.getColumns()) {
                    if (!columnExists(metaData, connection.getCatalog(), table.getTableName(), column.getColumnName())) {
                        statement.execute("ALTER TABLE " + table.getTableName() + " ADD COLUMN " + columnDefinition(column, false));
                    }
                }
                for (ColumnSchema runtimeColumn : runtimeColumns(resolveProjectId(design.getProjectId()))) {
                    if (!columnExists(metaData, connection.getCatalog(), table.getTableName(), runtimeColumn.getColumnName())) {
                        statement.execute("ALTER TABLE " + table.getTableName() + " ADD COLUMN " + columnDefinition(runtimeColumn, true));
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("业务表结构同步失败: " + e.getMessage(), e);
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String catalog, String tableName) throws SQLException {
        try (ResultSet tables = metaData.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private boolean columnExists(DatabaseMetaData metaData, String catalog, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metaData.getColumns(catalog, null, tableName, columnName)) {
            return columns.next();
        }
    }

    private String createTableSql(TableSchema table, Long projectId) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(table.getTableName()).append(" (\n");
        for (ColumnSchema column : table.getColumns()) {
            sql.append("    ").append(columnDefinition(column, false)).append(",\n");
        }
        List<ColumnSchema> runtimeColumns = runtimeColumns(projectId);
        for (int i = 0; i < runtimeColumns.size(); i++) {
            sql.append("    ").append(columnDefinition(runtimeColumns.get(i), true));
            sql.append(i == runtimeColumns.size() - 1 ? "\n" : ",\n");
        }
        sql.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='")
                .append(escapeSql(table.getTableComment())).append("'");
        return sql.toString();
    }

    private String columnDefinition(ColumnSchema column, boolean runtimeColumn) {
        StringBuilder definition = new StringBuilder();
        definition.append(column.getColumnName()).append(" ").append(column.getDataType());
        if (Boolean.TRUE.equals(column.getPrimaryKey())) {
            definition.append(" AUTO_INCREMENT PRIMARY KEY");
        } else {
            if (!Boolean.TRUE.equals(column.getNullable())) {
                definition.append(" NOT NULL");
            }
            String defaultValue = safeDefaultValue(column.getDefaultValue());
            if (defaultValue != null) {
                definition.append(" DEFAULT ").append(defaultValue);
            }
            if ("update_time".equals(column.getColumnName())) {
                definition.append(" ON UPDATE CURRENT_TIMESTAMP");
            }
        }
        definition.append(" COMMENT '").append(escapeSql(column.getComment())).append("'");
        return definition.toString();
    }

    private List<ColumnSchema> runtimeColumns(Long projectId) {
        return List.of(
                column("create_by", "BIGINT", "创建人", true, null),
                column("create_time", "DATETIME", "创建时间", true, "CURRENT_TIMESTAMP"),
                column("update_by", "BIGINT", "更新人", true, null),
                column("update_time", "DATETIME", "更新时间", true, "CURRENT_TIMESTAMP"),
                column("del_flag", "INT", "删除标识", true, "0"),
                column("project_id", "BIGINT", "项目ID", false, String.valueOf(resolveProjectId(projectId)))
        );
    }

    private void writeModulePackage(ModuleDesign design) {
        try {
            Path root = projectRoot();
            Path frontDir = root.resolve("src").resolve("modules").resolve(design.getModuleCode());
            Path backDir = root.resolve("server").resolve("modules").resolve(design.getModuleCode());
            Path backEntityDir = backDir.resolve("entity");
            Path backMapperDir = backDir.resolve("mapper");
            Path backServiceDir = backDir.resolve("service");
            Path backControllerDir = backDir.resolve("controller");
            
            Files.createDirectories(frontDir);
            Files.createDirectories(backEntityDir);
            Files.createDirectories(backMapperDir);
            Files.createDirectories(backServiceDir);
            Files.createDirectories(backControllerDir);

            String frontendCode = generateFrontendCode(design);
            String backendCode = generateBackendCode(design);
            String sqlScript = generateSqlScript(design);

            write(frontDir.resolve("module.json"), objectMapper.writeValueAsString(design));
            write(frontDir.resolve("menus.json"), objectMapper.writeValueAsString(design.getMenus()));
            write(frontDir.resolve("permissions.json"), objectMapper.writeValueAsString(design.getPermissions()));
            write(frontDir.resolve("routes.json"), objectMapper.writeValueAsString(design.getPages()));
            write(frontDir.resolve("api.js"), requiredGeneratedSection(frontendCode, "api.js"));
            write(frontDir.resolve("List.vue"), requiredGeneratedSection(frontendCode, "List.vue"));
            write(frontDir.resolve("Form.vue"), requiredGeneratedSection(frontendCode, "Form.vue"));
            write(frontDir.resolve("Detail.vue"), requiredGeneratedSection(frontendCode, "Detail.vue"));
            write(frontDir.resolve("generated-frontend.txt"), frontendCode);

            write(backDir.resolve("module.json"), objectMapper.writeValueAsString(design));
            write(backDir.resolve("init.sql"), sqlScript);
            write(backDir.resolve("generated-backend.txt"), backendCode);
            write(backEntityDir.resolve(upperCamel(design.getModuleCode()) + "Entity.java"), requiredGeneratedSection(backendCode, "Entity"));
            write(backMapperDir.resolve(upperCamel(design.getModuleCode()) + "Mapper.java"), requiredGeneratedSection(backendCode, "Mapper"));
            write(backServiceDir.resolve(upperCamel(design.getModuleCode()) + "Service.java"), requiredGeneratedSection(backendCode, "Service"));
            write(backControllerDir.resolve(upperCamel(design.getModuleCode()) + "Controller.java"), requiredGeneratedSection(backendCode, "Controller"));
        } catch (Exception e) {
            throw new IllegalStateException("模块文件生成失败: " + e.getMessage(), e);
        }
    }

    private String requiredGeneratedSection(String generatedCode, String sectionName) {
        String section = extractGeneratedSection(generatedCode, sectionName);
        if (!StringUtils.hasText(section)) {
            throw new IllegalStateException("AI 生成结果缺少分段: " + sectionName);
        }
        return section;
    }

    private String extractGeneratedSection(String generatedCode, String sectionName) {
        String marker = "// ========== " + sectionName + " ==========";
        String text = Objects.toString(generatedCode, "");
        int start = text.indexOf(marker);
        if (start < 0) {
            return "";
        }
        int contentStart = start + marker.length();
        int next = text.indexOf("// ==========", contentStart);
        String section = next >= 0 ? text.substring(contentStart, next) : text.substring(contentStart);
        return stripCodeFence(section).trim();
    }

    private Path projectRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        return cwd.getFileName() != null && cwd.getFileName().toString().equalsIgnoreCase("server")
                ? cwd.getParent()
                : cwd;
    }

    private void write(Path path, String content) throws Exception {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void validateDesign(ModuleDesign design) {
        if (design == null || design.getModuleCode() == null || design.getModuleName() == null) {
            throw new IllegalArgumentException("模块设计不能为空");
        }
        design.setModuleCode(sanitizeModuleCode(design.getModuleCode()));
        if (design.getTables() == null || design.getTables().isEmpty()) {
            throw new IllegalArgumentException("模块至少需要一张业务表");
        }
        normalizeGeneratedDesign(design);
    }

    private Long resolveProjectId(Long projectId) {
        return projectId == null || projectId <= 0 ? 1L : projectId;
    }

    private void normalizeGeneratedDesign(ModuleDesign design) {
        String moduleCode = design.getModuleCode();
        List<TableSchema> safeTables = new ArrayList<>();
        Set<String> tableNames = new java.util.LinkedHashSet<>();
        for (int i = 0; i < design.getTables().size(); i++) {
            TableSchema table = design.getTables().get(i);
            String tableName = sanitizeIdentifier(table.getTableName());
            if (!tableName.startsWith("biz_" + moduleCode + "_")) {
                tableName = i == 0 ? "biz_" + moduleCode + "_main" : "biz_" + moduleCode + "_" + i;
            }
            if (!tableNames.add(tableName)) {
                continue;
            }
            table.setTableName(tableName);
            table.setColumns(normalizeColumns(table.getColumns()));
            safeTables.add(table);
        }
        design.setTables(safeTables);
        requireRuntimeMetadata(design);
    }

    private List<ColumnSchema> normalizeColumns(List<ColumnSchema> columns) {
        Map<String, ColumnSchema> safeColumns = new LinkedHashMap<>();
        ColumnSchema id = column("id", "BIGINT", "主键", false, null);
        id.setPrimaryKey(true);
        id.setAutoIncrement(true);
        safeColumns.put("id", id);

        Set<String> runtimeColumns = Set.of("create_by", "create_time", "update_by", "update_time", "del_flag", "project_id");
        for (ColumnSchema column : Objects.requireNonNullElse(columns, List.<ColumnSchema>of())) {
            String columnName = sanitizeIdentifier(column.getColumnName());
            if (runtimeColumns.contains(columnName) || columnName.isBlank() || safeColumns.containsKey(columnName)) {
                continue;
            }
            column.setColumnName(columnName);
            column.setDataType(safeDataType(column.getDataType()));
            column.setComment(Objects.toString(column.getComment(), columnName));
            column.setPrimaryKey(false);
            column.setAutoIncrement(false);
            safeColumns.put(columnName, column);
        }
        if (safeColumns.size() == 1) {
            throw new IllegalArgumentException("AI 生成的业务表缺少业务字段");
        }
        return new ArrayList<>(safeColumns.values());
    }

    private void requireRuntimeMetadata(ModuleDesign design) {
        if (design.getPages() == null || design.getPages().isEmpty()) {
            throw new IllegalArgumentException("模块设计缺少页面配置");
        }
        if (design.getApis() == null || design.getApis().isEmpty()) {
            throw new IllegalArgumentException("模块设计缺少 API 配置");
        }
        if (design.getPermissions() == null || design.getPermissions().isEmpty()) {
            throw new IllegalArgumentException("模块设计缺少权限配置");
        }
        if (design.getMenus() == null || design.getMenus().isEmpty()) {
            throw new IllegalArgumentException("模块设计缺少菜单配置");
        }
    }

    private String sanitizeIdentifier(String value) {
        String normalized = Objects.toString(value, "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (!normalized.matches("[a-z][a-z0-9_]{0,60}")) {
            return "";
        }
        return normalized;
    }

    private String safeDataType(String dataType) {
        String normalized = Objects.toString(dataType, "VARCHAR(100)").toUpperCase(Locale.ROOT).trim();
        if (normalized.matches("VARCHAR\\([1-9][0-9]{0,3}\\)")
                || normalized.matches("CHAR\\([1-9][0-9]{0,3}\\)")
                || normalized.matches("DECIMAL\\([1-9][0-9]?,[0-9]+\\)")
                || normalized.equals("BIGINT")
                || normalized.equals("INT")
                || normalized.equals("DATE")
                || normalized.equals("DATETIME")
                || normalized.equals("TEXT")) {
            return normalized;
        }
        return "VARCHAR(100)";
    }

    private String safeDefaultValue(String defaultValue) {
        if (defaultValue == null || defaultValue.isBlank()) {
            return null;
        }
        String trimmed = defaultValue.trim();
        if (trimmed.matches("-?[0-9]+(\\.[0-9]+)?")
                || trimmed.matches("'[^'\\\\]{0,100}'")
                || trimmed.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
            return trimmed;
        }
        return null;
    }

    private String sanitizeModuleCode(String moduleCode) {
        String normalized = Objects.toString(moduleCode, "module").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (!normalized.matches("[a-z][a-z0-9_]{1,40}")) {
            return "module";
        }
        return normalized;
    }

    private String upperCamel(String value) {
        StringBuilder builder = new StringBuilder();
        for (String part : value.split("_")) {
            if (!part.isEmpty()) {
                builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private String escapeSql(String value) {
        return Objects.toString(value, "").replace("'", "''");
    }
}
