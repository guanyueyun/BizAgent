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
                你是企业低代码平台的需求优化助手。
                请在不改变用户原意的前提下，把自然语言需求优化成更适合生成业务模块的描述。
                必须只输出优化后的中文需求文本，不要 Markdown，不要代码块，不要解释。
                优化内容应包含：业务目标、核心对象、页面、字段、流程、权限、统计、审批、导入导出、移动端等。
                只能描述当前平台内业务功能模块，禁止要求生成完整系统、登录系统、权限系统或外部项目。
                如果原需求缺少信息，用“待确认：...”列出，不能凭空确定。
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
                你是企业低代码平台业务模块生成引擎。
                你只能生成当前平台内部业务功能模块，禁止生成完整系统、登录系统、权限系统、基础架构或外部项目。
                输出必须是严格 JSON，不要 Markdown，不要解释。
                JSON 字段必须符合 ModuleDesign：moduleName, moduleCode, description, tables, pages, apis, permissions, menus。
                表名必须使用 biz_{moduleCode}_ 前缀，主业务表必须是 biz_{moduleCode}_main。
                权限编码必须符合 模块:操作，例如 inspection:list。
                所有业务表必须能补充 id/create_by/create_time/update_by/update_time/del_flag/project_id 通用字段。
                字段名、表名、moduleCode 只能使用小写英文、数字和下划线，moduleCode 必须以英文字母开头。
                必须至少包含 list/add/edit/detail 四类页面，以及 list/add/edit/delete 四类权限。
                如果需求包含审批、导入导出、统计、消息提醒，必须在 apis 和 permissions 中同步体现。
                """;
    }

    private String userPrompt(RequirementRequest request) throws Exception {
        return "请根据以下需求生成平台业务模块设计 JSON：\n" + objectMapper.writeValueAsString(request);
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

    private String extractModuleCode(String requirement) {
        if (requirement.contains("设备巡检")) return "inspection";
        if (requirement.contains("仓储") || requirement.toUpperCase(Locale.ROOT).contains("WMS")) return "wms";
        if (requirement.contains("安全") || requirement.contains("质量")) return "quality";
        if (requirement.contains("合同")) return "contract";
        if (requirement.contains("物资")) return "material";
        if (requirement.contains("请假")) return "leave";
        if (requirement.contains("人员") || requirement.contains("员工")) return "personnel";
        if (requirement.contains("审批流程") || requirement.contains("审批管理")) return "approval";
        return "mod_" + Integer.toUnsignedString(requirement.hashCode(), 36);
    }

    private String extractModuleName(String requirement) {
        if (requirement.contains("设备巡检")) return "设备巡检";
        if (requirement.contains("仓储") || requirement.toUpperCase(Locale.ROOT).contains("WMS")) return "仓储管理";
        if (requirement.contains("安全") || requirement.contains("质量")) return "安全质量";
        if (requirement.contains("合同")) return "合同管理";
        if (requirement.contains("物资")) return "物资管理";
        if (requirement.contains("请假")) return "请假管理";
        if (requirement.contains("人员") || requirement.contains("员工")) return "人员管理";
        if (requirement.contains("审批流程") || requirement.contains("审批管理")) return "审批流程";
        return "业务模块";
    }

    private List<TableSchema> generateTables(String moduleCode, RequirementRequest request) {
        List<TableSchema> tables = new ArrayList<>();
        tables.add(mainBusinessTable(moduleCode, Objects.toString(request.getRequirement(), "")));

        if (Boolean.TRUE.equals(request.getNeedApproval()) || moduleCode.equals("approval") || moduleCode.equals("leave")) {
            tables.add(table("biz_" + moduleCode + "_approval", "审批记录",
                    column("biz_id", "BIGINT", "业务数据ID", false, null),
                    column("node_name", "VARCHAR(100)", "审批节点", false, null),
                    column("approver", "VARCHAR(100)", "审批人", true, null),
                    column("approval_status", "VARCHAR(30)", "审批状态", false, "'pending'"),
                    column("approval_comment", "VARCHAR(500)", "审批意见", true, null),
                    column("approval_time", "DATETIME", "审批时间", true, null)));
        }

        if (Boolean.TRUE.equals(request.getNeedStatistics())) {
            tables.add(table("biz_" + moduleCode + "_stat_day", "日统计",
                    column("stat_date", "DATE", "统计日期", false, null),
                    column("total_count", "INT", "总数量", false, "0"),
                    column("done_count", "INT", "完成数量", false, "0"),
                    column("exception_count", "INT", "异常数量", false, "0")));
        }
        if (Boolean.TRUE.equals(request.getNeedNotification())) {
            tables.add(table("biz_" + moduleCode + "_notification", "消息提醒",
                    column("biz_id", "BIGINT", "业务数据ID", false, null),
                    column("receiver", "VARCHAR(100)", "接收人", true, null),
                    column("message_title", "VARCHAR(200)", "消息标题", false, null),
                    column("message_content", "VARCHAR(500)", "消息内容", true, null),
                    column("read_status", "VARCHAR(30)", "阅读状态", false, "'unread'")));
        }

        return tables;
    }

    private TableSchema mainBusinessTable(String moduleCode, String requirement) {
        if (moduleCode.equals("inspection")) {
            return table("biz_inspection_main", "设备巡检主表",
                    column("inspection_no", "VARCHAR(50)", "巡检单号", false, null),
                    column("device_name", "VARCHAR(100)", "设备名称", false, null),
                    column("device_code", "VARCHAR(50)", "设备编码", true, null),
                    column("location", "VARCHAR(100)", "位置", true, null),
                    column("inspection_plan", "VARCHAR(200)", "巡检计划", true, null),
                    column("inspector", "VARCHAR(100)", "巡检人", true, null),
                    column("inspection_time", "DATETIME", "巡检时间", true, null),
                    column("exception_desc", "VARCHAR(500)", "异常说明", true, null),
                    column("status", "VARCHAR(30)", "状态", false, "'draft'"));
        }
        if (moduleCode.equals("wms")) {
            return table("biz_wms_main", "仓储管理主表",
                    column("warehouse_name", "VARCHAR(100)", "仓库名称", false, null),
                    column("material_code", "VARCHAR(50)", "物料编码", false, null),
                    column("material_name", "VARCHAR(100)", "物料名称", false, null),
                    column("supplier_name", "VARCHAR(100)", "供应商", true, null),
                    column("quantity", "DECIMAL(18,2)", "数量", false, "0"),
                    column("inbound_time", "DATETIME", "入库时间", true, null),
                    column("status", "VARCHAR(30)", "状态", false, "'normal'"));
        }
        if (moduleCode.equals("leave")) {
            return table("biz_leave_main", "请假管理主表",
                    column("employee_name", "VARCHAR(100)", "员工姓名", false, null),
                    column("leave_type", "VARCHAR(50)", "请假类型", false, null),
                    column("start_time", "DATETIME", "开始时间", false, null),
                    column("end_time", "DATETIME", "结束时间", false, null),
                    column("reason", "VARCHAR(500)", "请假原因", true, null),
                    column("status", "VARCHAR(30)", "状态", false, "'draft'"));
        }
        List<ColumnSchema> columns = new ArrayList<>();
        columns.add(column("biz_no", "VARCHAR(50)", "业务编号", false, null));
        columns.add(column("name", "VARCHAR(100)", "名称", false, null));
        columns.add(column("owner_name", "VARCHAR(100)", "负责人", true, null));
        columns.addAll(extractCustomColumns(requirement));
        columns.add(column("status", "VARCHAR(30)", "状态", false, "'draft'"));
        columns.add(column("remark", "VARCHAR(500)", "备注", true, null));
        return table("biz_" + moduleCode + "_main", moduleCode + "主表", columns.toArray(new ColumnSchema[0]));
    }

    private List<ColumnSchema> extractCustomColumns(String requirement) {
        List<ColumnSchema> columns = new ArrayList<>();
        addColumnIfMentioned(columns, requirement, "客户", "customer_name", "客户名称", "VARCHAR(100)");
        addColumnIfMentioned(columns, requirement, "联系人", "contact_name", "联系人", "VARCHAR(100)");
        addColumnIfMentioned(columns, requirement, "电话", "contact_phone", "联系电话", "VARCHAR(30)");
        addColumnIfMentioned(columns, requirement, "金额", "amount", "金额", "DECIMAL(18,2)");
        addColumnIfMentioned(columns, requirement, "日期", "biz_date", "业务日期", "DATE");
        addColumnIfMentioned(columns, requirement, "时间", "biz_time", "业务时间", "DATETIME");
        addColumnIfMentioned(columns, requirement, "附件", "attachment_url", "附件地址", "VARCHAR(500)");
        addColumnIfMentioned(columns, requirement, "地址", "address", "地址", "VARCHAR(255)");
        return columns;
    }

    private void addColumnIfMentioned(List<ColumnSchema> columns, String requirement, String keyword, String name, String comment, String type) {
        if (requirement.contains(keyword) && columns.stream().noneMatch(column -> column.getColumnName().equals(name))) {
            columns.add(column(name, type, comment, true, null));
        }
    }

    private TableSchema table(String tableName, String tableComment, ColumnSchema... columns) {
        TableSchema table = new TableSchema();
        table.setTableName(tableName);
        table.setTableComment(tableComment);
        List<ColumnSchema> allColumns = new ArrayList<>();
        ColumnSchema id = column("id", "BIGINT", "主键", false, null);
        id.setPrimaryKey(true);
        id.setAutoIncrement(true);
        allColumns.add(id);
        allColumns.addAll(List.of(columns));
        table.setColumns(allColumns);
        return table;
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

    private List<PageSchema> generatePages(String moduleCode, String moduleName, RequirementRequest request, List<TableSchema> tables) {
        List<PageSchema> pages = new ArrayList<>();
        List<FieldSchema> fields = generateFields(tables.get(0));
        pages.add(page(moduleName + "列表", "list", "/" + moduleCode + "/list", "modules/" + moduleCode + "/List", fields));
        pages.add(page(moduleName + "新增", "add", "/" + moduleCode + "/add", "modules/" + moduleCode + "/Form", fields));
        pages.add(page(moduleName + "编辑", "edit", "/" + moduleCode + "/edit/:id", "modules/" + moduleCode + "/Form", fields));
        pages.add(page(moduleName + "详情", "detail", "/" + moduleCode + "/detail/:id", "modules/" + moduleCode + "/Detail", fields));
        if (Boolean.TRUE.equals(request.getNeedStatistics())) {
            pages.add(page(moduleName + "统计", "statistics", "/" + moduleCode + "/statistics", "modules/" + moduleCode + "/Statistics", fields));
        }
        if (Boolean.TRUE.equals(request.getNeedApproval())) {
            pages.add(page(moduleName + "审批", "approval", "/" + moduleCode + "/approval", "modules/" + moduleCode + "/Approval", fields));
        }
        if (Boolean.TRUE.equals(request.getNeedMobile())) {
            pages.add(page(moduleName + "移动端", "mobile", "/" + moduleCode + "/mobile", "modules/" + moduleCode + "/Mobile", fields));
        }
        return pages;
    }

    private PageSchema page(String name, String type, String path, String component, List<FieldSchema> fields) {
        PageSchema page = new PageSchema();
        page.setPageName(name);
        page.setPageType(type);
        page.setPath(path);
        page.setComponent(component);
        page.setFields(fields);
        return page;
    }

    private List<FieldSchema> generateFields(TableSchema table) {
        return table.getColumns().stream()
                .filter(column -> !Boolean.TRUE.equals(column.getPrimaryKey()))
                .map(this::fieldFromColumn)
                .toList();
    }

    private FieldSchema fieldFromColumn(ColumnSchema column) {
        FieldSchema field = new FieldSchema();
        field.setFieldName(column.getColumnName());
        field.setLabel(column.getComment());
        field.setRequired(!Boolean.TRUE.equals(column.getNullable()));
        field.setPlaceholder("请输入" + column.getComment());
        field.setType(resolveFieldType(column));
        if ("select".equals(field.getType())) {
            field.setOptions("draft:草稿,submitted:已提交,approved:已通过,rejected:已驳回,normal:正常,disabled:停用");
        }
        return field;
    }

    private String resolveFieldType(ColumnSchema column) {
        String name = column.getColumnName();
        String dataType = column.getDataType().toUpperCase(Locale.ROOT);
        if ("status".equals(name) || name.endsWith("_status")) return "select";
        if (dataType.contains("DATETIME")) return "datetime";
        if (dataType.equals("DATE")) return "date";
        if (dataType.contains("INT") || dataType.contains("DECIMAL")) return "number";
        if (dataType.contains("500") || name.contains("remark") || name.contains("desc") || name.contains("reason")) return "textarea";
        return "input";
    }

    private List<ApiSchema> generateApis(String moduleCode, RequirementRequest request) {
        List<ApiSchema> apis = new ArrayList<>();
        apis.add(api("分页查询", "GET", "/api/biz/" + moduleCode + "/list"));
        apis.add(api("新增", "POST", "/api/biz/" + moduleCode));
        apis.add(api("修改", "PUT", "/api/biz/" + moduleCode + "/{id}"));
        apis.add(api("删除", "DELETE", "/api/biz/" + moduleCode + "/{id}"));
        apis.add(api("详情", "GET", "/api/biz/" + moduleCode + "/{id}"));
        if (Boolean.TRUE.equals(request.getNeedImportExport())) {
            apis.add(api("导入", "POST", "/api/biz/" + moduleCode + "/import"));
            apis.add(api("导出", "GET", "/api/biz/" + moduleCode + "/export"));
        }
        if (Boolean.TRUE.equals(request.getNeedStatistics())) {
            apis.add(api("统计", "GET", "/api/biz/" + moduleCode + "/statistics"));
        }
        if (Boolean.TRUE.equals(request.getNeedApproval())) {
            apis.add(api("提交审批", "POST", "/api/biz/" + moduleCode + "/{id}/submit"));
            apis.add(api("审批处理", "POST", "/api/biz/" + moduleCode + "/{id}/approve"));
        }
        if (Boolean.TRUE.equals(request.getNeedNotification())) {
            apis.add(api("消息提醒", "POST", "/api/biz/" + moduleCode + "/{id}/notify"));
            apis.add(api("提醒列表", "GET", "/api/biz/" + moduleCode + "/notifications"));
        }
        return apis;
    }

    private ApiSchema api(String name, String method, String path) {
        ApiSchema api = new ApiSchema();
        api.setApiName(name);
        api.setMethod(method);
        api.setPath(path);
        api.setDescription(name + "接口");
        return api;
    }

    private List<PermissionSchema> generatePermissions(String moduleCode, String moduleName, RequirementRequest request) {
        List<PermissionSchema> permissions = new ArrayList<>();
        addPermission(permissions, moduleCode, moduleName, "list", "查看");
        addPermission(permissions, moduleCode, moduleName, "add", "新增");
        addPermission(permissions, moduleCode, moduleName, "edit", "编辑");
        addPermission(permissions, moduleCode, moduleName, "delete", "删除");
        if (Boolean.TRUE.equals(request.getNeedImportExport())) {
            addPermission(permissions, moduleCode, moduleName, "import", "导入");
            addPermission(permissions, moduleCode, moduleName, "export", "导出");
        }
        if (Boolean.TRUE.equals(request.getNeedStatistics())) {
            addPermission(permissions, moduleCode, moduleName, "statistics", "统计");
        }
        if (Boolean.TRUE.equals(request.getNeedApproval())) {
            addPermission(permissions, moduleCode, moduleName, "approve", "审批");
        }
        if (Boolean.TRUE.equals(request.getNeedNotification())) {
            addPermission(permissions, moduleCode, moduleName, "notify", "提醒");
        }
        return permissions;
    }

    private void addPermission(List<PermissionSchema> permissions, String moduleCode, String moduleName, String action, String actionName) {
        PermissionSchema permission = new PermissionSchema();
        permission.setPermissionCode(moduleCode + ":" + action);
        permission.setPermissionName(moduleName + actionName);
        permission.setModuleName(moduleName);
        permissions.add(permission);
    }

    private List<MenuSchema> generateMenus(String moduleCode, String moduleName) {
        List<MenuSchema> menus = new ArrayList<>();
        MenuSchema mainMenu = new MenuSchema();
        mainMenu.setMenuName(moduleName);
        mainMenu.setPath("/module-runtime/" + moduleCode);
        mainMenu.setComponent("ModuleRuntime");
        mainMenu.setParentId(0L);
        mainMenu.setIcon("component");
        mainMenu.setSortOrder(100);
        mainMenu.setMenuType(2);
        menus.add(mainMenu);
        return menus;
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
            return "请基于以下 ModuleDesign 生成代码。ModuleDesign JSON：\n" + objectMapper.writeValueAsString(design);
        } catch (Exception e) {
            return "请基于模块编码 " + design.getModuleCode() + " 和模块名称 " + design.getModuleName() + " 生成代码。";
        }
    }

    private String frontendCodePrompt() {
        return """
                你是 BizAgent 平台的 Vue3 + Element Plus 前端代码生成器。
                必须基于输入的 ModuleDesign 生成真实可用的模块前端代码，不要复述需求。
                只输出代码文本，不要 Markdown，不要解释。
                输出格式必须分段包含：
                // ========== api.js ==========
                // ========== List.vue ==========
                // ========== Form.vue ==========
                // ========== Detail.vue ==========
                如果 ModuleDesign 包含 statistics/approval/mobile 页面，也继续输出对应 Vue 文件分段。
                代码要求：
                1. 使用 Vue3 script setup 和 Element Plus。
                2. api.js 从 ../../api 导入 request，并请求 /biz/{moduleCode} 系列接口。
                3. 表格、表单、详情字段必须来自主表字段，排除 id/create_by/create_time/update_by/update_time/del_flag/project_id 等系统字段。
                4. 必填、日期、数字、下拉、textarea 控件要按字段类型生成。
                5. 不生成登录、权限系统、独立项目或新的基础架构。
                """;
    }

    private String backendCodePrompt() {
        return """
                你是 BizAgent 平台的 Spring Boot 3 + MyBatis Plus 后端代码生成器。
                必须基于输入的 ModuleDesign 生成模块后端草案代码，不要复述需求。
                只输出代码文本，不要 Markdown，不要解释。
                输出格式必须分段包含：
                // ========== Entity ==========
                // ========== Mapper ==========
                // ========== Service ==========
                // ========== Controller ==========
                代码要求：
                1. package 使用 com.example.bizagent.modules.{moduleCode}.*
                2. Entity 使用 @TableName 指向主业务表，包含主表业务字段和 id/createBy/createTime/updateBy/updateTime/delFlag/projectId。
                3. Controller 路径使用 /api/biz/{moduleCode}，提供 list/detail/create/update/delete。
                4. 查询必须过滤 delFlag=0 和 projectId，删除优先软删除。
                5. 不生成登录、权限系统、独立项目或新的基础架构。
                """;
    }

    private String sqlCodePrompt() {
        return """
                你是 BizAgent 平台的 MySQL 8 SQL 生成器。
                必须基于输入的 ModuleDesign 生成可执行 SQL，不要复述需求。
                只输出 SQL，不要 Markdown，不要解释。
                SQL 要求：
                1. 每张表使用 CREATE TABLE IF NOT EXISTS。
                2. 表名必须严格来自 ModuleDesign，不得新增系统表。
                3. 每张业务表必须包含 id BIGINT AUTO_INCREMENT PRIMARY KEY。
                4. 每张业务表必须包含 create_by、create_time、update_by、update_time、del_flag、project_id。
                5. 使用 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4，中文 COMMENT 必须保留。
                6. 禁止 DROP、TRUNCATE、DELETE、UPDATE、ALTER 系统表。
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

    private String moduleApiJs(ModuleDesign design) {
        return """
                import request from '../../api'

                export const moduleCode = '%s'
                export const list = (params) => request.get(`/biz/${moduleCode}/list`, { params })
                export const detail = (id) => request.get(`/biz/${moduleCode}/${id}`)
                export const create = (data) => request.post(`/biz/${moduleCode}`, data)
                export const update = (id, data) => request.put(`/biz/${moduleCode}/${id}`, data)
                export const remove = (id) => request.delete(`/biz/${moduleCode}/${id}`)
                """.formatted(design.getModuleCode());
    }

    private String moduleListVue(ModuleDesign design) {
        String columns = design.getTables().get(0).getColumns().stream()
                .filter(column -> !Boolean.TRUE.equals(column.getPrimaryKey()))
                .limit(6)
                .map(column -> "      <el-table-column prop=\"" + column.getColumnName() + "\" label=\"" + column.getComment() + "\" />")
                .reduce("", (left, right) -> left + right + "\n");
        return """
                <template>
                  <div class="generated-module">
                    <div class="module-toolbar">
                      <h2>%s</h2>
                      <el-button type="primary" @click="handleAdd">新增</el-button>
                    </div>
                    <el-table :data="tableData" border @row-click="handleView">
                %s      <el-table-column prop="id" label="ID" width="80" />
                      <el-table-column label="操作" width="180">
                        <template #default="scope">
                          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
                          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                    <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
                      :current-page="pageNum" :page-sizes="[10, 20, 50]" :page-size="pageSize"
                      :total="total" layout="total, sizes, prev, pager, next, jumper" />
                  </div>
                </template>

                <script setup>
                import { ref, onMounted } from 'vue'
                import * as api from './api'

                defineOptions({ name: '%sList' })

                const tableData = ref([])
                const pageNum = ref(1)
                const pageSize = ref(10)
                const total = ref(0)

                const loadData = async () => {
                  const res = await api.list({ pageNum: pageNum.value, pageSize: pageSize.value })
                  tableData.value = res.data.data.records
                  total.value = res.data.data.total
                }

                const handleSizeChange = (val) => { pageSize.value = val; loadData() }
                const handleCurrentChange = (val) => { pageNum.value = val; loadData() }
                const handleAdd = () => { }
                const handleEdit = (row) => { }
                const handleDelete = (row) => { }
                const handleView = (row) => { }

                onMounted(() => loadData())
                </script>

                <style scoped>
                .generated-module { padding: 20px; }
                .module-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
                </style>
                """.formatted(design.getModuleName(), columns, upperCamel(design.getModuleCode()));
    }

    private String moduleFormVue(ModuleDesign design) {
        String fields = design.getTables().get(0).getColumns().stream()
                .filter(column -> !Boolean.TRUE.equals(column.getPrimaryKey()) && !column.getColumnName().startsWith("create_") && !column.getColumnName().startsWith("update_"))
                .map(column -> {
                    String fieldType = resolveFieldType(column);
                    String required = !Boolean.TRUE.equals(column.getNullable()) ? "required" : "";
                    if ("textarea".equals(fieldType)) {
                        return "      <el-form-item label=\"" + column.getComment() + "\" " + required + "><el-input v-model=\"form." + column.getColumnName() + "\" type=\"textarea\" :rows=\"3\" /></el-form-item>";
                    } else if ("select".equals(fieldType)) {
                        return "      <el-form-item label=\"" + column.getComment() + "\" " + required + "><el-select v-model=\"form." + column.getColumnName() + "\"><el-option label=\"草稿\" value=\"draft\" /><el-option label=\"已提交\" value=\"submitted\" /><el-option label=\"已通过\" value=\"approved\" /></el-select></el-form-item>";
                    } else if ("date".equals(fieldType)) {
                        return "      <el-form-item label=\"" + column.getComment() + "\" " + required + "><el-date-picker v-model=\"form." + column.getColumnName() + "\" type=\"date\" style=\"width:100%\" /></el-form-item>";
                    } else if ("datetime".equals(fieldType)) {
                        return "      <el-form-item label=\"" + column.getComment() + "\" " + required + "><el-date-picker v-model=\"form." + column.getColumnName() + "\" type=\"datetime\" style=\"width:100%\" /></el-form-item>";
                    } else if ("number".equals(fieldType)) {
                        return "      <el-form-item label=\"" + column.getComment() + "\" " + required + "><el-input v-model.number=\"form." + column.getColumnName() + "\" type=\"number\" /></el-form-item>";
                    }
                    return "      <el-form-item label=\"" + column.getComment() + "\" " + required + "><el-input v-model=\"form." + column.getColumnName() + "\" /></el-form-item>";
                })
                .reduce("", (left, right) -> left + right + "\n");
        return """
                <template>
                  <div class="generated-module">
                    <el-form ref="formRef" :model="form" label-width="120px">
                %s    </el-form>
                    <div style="margin-top: 20px; text-align: right;">
                      <el-button @click="handleCancel">取消</el-button>
                      <el-button type="primary" @click="handleSubmit">保存</el-button>
                    </div>
                  </div>
                </template>

                <script setup>
                import { ref, reactive, onMounted } from 'vue'
                import * as api from './api'

                defineOptions({ name: '%sForm' })

                const props = defineProps({ id: [Number, String] })
                const emit = defineEmits(['close'])

                const formRef = ref(null)
                const form = reactive({})

                const initForm = () => {
                }

                const handleSubmit = async () => {
                  if (props.id) await api.update(props.id, form)
                  else await api.create(form)
                  emit('close')
                }

                const handleCancel = () => emit('close')

                onMounted(() => {
                  initForm()
                  if (props.id) api.detail(props.id).then(res => Object.assign(form, res.data.data))
                })
                </script>

                <style scoped>
                .generated-module { padding: 20px; }
                </style>
                """.formatted(fields, upperCamel(design.getModuleCode()));
    }

    private String moduleDetailVue(ModuleDesign design) {
        String fields = design.getTables().get(0).getColumns().stream()
                .filter(column -> !Boolean.TRUE.equals(column.getPrimaryKey()))
                .map(column -> "      <el-descriptions-item label=\"" + column.getComment() + "\">{{ form." + column.getColumnName() + " || '-' }}</el-descriptions-item>")
                .reduce("", (left, right) -> left + right + "\n");
        return """
                <template>
                  <div class="generated-module">
                    <el-descriptions title="%s详情" :column="2" border>
                %s    </el-descriptions>
                    <div style="margin-top: 20px; text-align: right;">
                      <el-button @click="handleEdit">编辑</el-button>
                      <el-button @click="handleBack">返回</el-button>
                    </div>
                  </div>
                </template>

                <script setup>
                import { reactive, onMounted } from 'vue'
                import * as api from './api'

                defineOptions({ name: '%sDetail' })

                const props = defineProps({ id: [Number, String] })
                const emit = defineEmits(['back', 'edit'])

                const form = reactive({})

                const handleEdit = () => emit('edit', props.id)
                const handleBack = () => emit('back')

                onMounted(() => {
                  if (props.id) api.detail(props.id).then(res => Object.assign(form, res.data.data))
                })
                </script>

                <style scoped>
                .generated-module { padding: 20px; }
                </style>
                """.formatted(design.getModuleName(), fields, upperCamel(design.getModuleCode()));
    }

    private String moduleEntity(ModuleDesign design) {
        String className = upperCamel(design.getModuleCode()) + "Entity";
        String fields = design.getTables().get(0).getColumns().stream()
                .filter(column -> !column.getColumnName().equals("id") && !column.getColumnName().equals("create_by") && !column.getColumnName().equals("create_time") && !column.getColumnName().equals("update_by") && !column.getColumnName().equals("update_time") && !column.getColumnName().equals("del_flag") && !column.getColumnName().equals("project_id"))
                .map(column -> {
                    String javaType = resolveJavaType(column.getDataType());
                    return "    private " + javaType + " " + column.getColumnName() + "; // " + column.getComment();
                })
                .reduce("", (left, right) -> left + right + "\n");
        return """
                package com.example.bizagent.modules.%s.entity;

                import com.baomidou.mybatisplus.annotation.IdType;
                import com.baomidou.mybatisplus.annotation.TableField;
                import com.baomidou.mybatisplus.annotation.TableId;
                import com.baomidou.mybatisplus.annotation.TableName;
                import lombok.Data;

                @Data
                @TableName("%s")
                public class %s {

                    @TableId(type = IdType.AUTO)
                    private Long id;

                %s    private Long createBy;
                    private java.time.LocalDateTime createTime;
                    private Long updateBy;
                    private java.time.LocalDateTime updateTime;
                    private Integer delFlag;
                    private Long projectId;
                }
                """.formatted(design.getModuleCode(), design.getTables().get(0).getTableName(), className, fields);
    }

    private String moduleMapper(ModuleDesign design) {
        String className = upperCamel(design.getModuleCode()) + "Mapper";
        String entityName = upperCamel(design.getModuleCode()) + "Entity";
        return """
                package com.example.bizagent.modules.%s.mapper;

                import com.baomidou.mybatisplus.core.mapper.BaseMapper;
                import com.example.bizagent.modules.%s.entity.%s;
                import org.apache.ibatis.annotations.Mapper;

                @Mapper
                public interface %s extends BaseMapper<%s> {
                }
                """.formatted(design.getModuleCode(), design.getModuleCode(), entityName, className, entityName);
    }

    private String moduleService(ModuleDesign design) {
        String className = upperCamel(design.getModuleCode()) + "Service";
        String entityName = upperCamel(design.getModuleCode()) + "Entity";
        String mapperName = upperCamel(design.getModuleCode()) + "Mapper";
        return """
                package com.example.bizagent.modules.%s.service;

                import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
                import com.example.bizagent.modules.%s.entity.%s;
                import com.example.bizagent.modules.%s.mapper.%s;
                import org.springframework.stereotype.Service;

                @Service
                public class %s extends ServiceImpl<%s, %s> {

                    public %s(%s mapper) {
                        super(mapper);
                    }
                }
                """.formatted(design.getModuleCode(), design.getModuleCode(), entityName, design.getModuleCode(), mapperName, className, mapperName, entityName, className, mapperName);
    }

    private String moduleController(ModuleDesign design) {
        String className = upperCamel(design.getModuleCode()) + "Controller";
        String serviceName = upperCamel(design.getModuleCode()) + "Service";
        String entityName = upperCamel(design.getModuleCode()) + "Entity";
        return """
                package com.example.bizagent.modules.%s.controller;

                import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
                import com.baomidou.mybatisplus.core.metadata.IPage;
                import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
                import com.example.bizagent.common.PageResponse;
                import com.example.bizagent.common.ResponseEntity;
                import com.example.bizagent.modules.%s.entity.%s;
                import com.example.bizagent.modules.%s.service.%s;
                import org.springframework.web.bind.annotation.*;

                @RestController
                @RequestMapping("/api/biz/%s")
                public class %s {

                    private final %s service;

                    public %s(%s service) {
                        this.service = service;
                    }

                    @GetMapping("/list")
                    public ResponseEntity<PageResponse<%s>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "10") Integer pageSize) {
                        IPage<%s> page = service.page(new Page<>(pageNum, pageSize),
                            new LambdaQueryWrapper<%s>().eq(%s::getDelFlag, 0).orderByDesc(%s::getCreateTime));
                        return ResponseEntity.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
                    }

                    @GetMapping("/{id}")
                    public ResponseEntity<%s> get(@PathVariable Long id) {
                        return ResponseEntity.success(service.getById(id));
                    }

                    @PostMapping
                    public ResponseEntity<%s> create(@RequestBody %s entity) {
                        service.save(entity);
                        return ResponseEntity.success("新增成功", entity);
                    }

                    @PutMapping("/{id}")
                    public ResponseEntity<%s> update(@PathVariable Long id, @RequestBody %s entity) {
                        entity.setId(id);
                        service.updateById(entity);
                        return ResponseEntity.success("修改成功", entity);
                    }

                    @DeleteMapping("/{id}")
                    public ResponseEntity<Void> delete(@PathVariable Long id) {
                        service.removeById(id);
                        return ResponseEntity.success("删除成功");
                    }
                }
                """.formatted(
                        design.getModuleCode(), design.getModuleCode(), entityName,
                        design.getModuleCode(), serviceName, design.getModuleCode(),
                        className, serviceName, className, serviceName,
                        entityName, entityName, entityName, entityName, entityName,
                        entityName, entityName, entityName, entityName, entityName);
    }

    private String resolveJavaType(String dataType) {
        String type = Objects.toString(dataType, "").toUpperCase(Locale.ROOT).trim();
        if (type.contains("BIGINT")) return "Long";
        if (type.contains("INT")) return "Integer";
        if (type.contains("DECIMAL")) return "java.math.BigDecimal";
        if (type.equals("DATE")) return "java.time.LocalDate";
        if (type.contains("DATETIME")) return "java.time.LocalDateTime";
        if (type.equals("TEXT")) return "String";
        return "String";
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
