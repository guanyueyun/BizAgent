package com.example.bizagent.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.system.entity.SysModelConfig;
import com.example.bizagent.modules.system.service.SysModelConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/model-config")
public class SysModelConfigController {

    private static final List<ProviderPreset> PROVIDER_PRESETS = List.of(
            new ProviderPreset("OpenAI-Compatible", "OpenAI 兼容", "https://api.openai.com/v1",
                    List.of("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("OpenAI", "OpenAI", "https://api.openai.com/v1",
                    List.of("gpt-4o-mini", "gpt-4o", "gpt-4.1", "gpt-4.1-mini"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("Azure OpenAI", "Azure OpenAI", "https://{resource}.openai.azure.com/openai/deployments/{deployment}",
                    List.of("gpt-4o-mini", "gpt-4o", "gpt-4.1"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("DeepSeek", "DeepSeek", "https://api.deepseek.com/v1",
                    List.of("deepseek-chat", "deepseek-reasoner"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("通义千问", "阿里云通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1",
                    List.of("qwen-plus", "qwen-max", "qwen-turbo", "qwq-plus"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("智谱 GLM", "智谱 GLM", "https://open.bigmodel.cn/api/paas/v4",
                    List.of("glm-4-plus", "glm-4-flash", "glm-4-air"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("Moonshot", "月之暗面 Moonshot", "https://api.moonshot.cn/v1",
                    List.of("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("MiniMax", "MiniMax", "https://api.minimax.chat/v1",
                    List.of("abab6.5s-chat", "abab6.5g-chat", "abab6.5t-chat"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("百川智能", "百川智能", "https://api.baichuan-ai.com/v1",
                    List.of("Baichuan4", "Baichuan3-Turbo", "Baichuan3-Turbo-128k"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("零一万物", "零一万物", "https://api.lingyiwanwu.com/v1",
                    List.of("yi-large", "yi-medium", "yi-lightning"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("讯飞星火", "讯飞星火", "https://spark-api-open.xf-yun.com/v1",
                    List.of("generalv3.5", "generalv4.0", "lite"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("百度千帆", "百度智能云千帆", "https://qianfan.baidubce.com/v2",
                    List.of("ernie-4.0-8k", "ernie-3.5-8k", "ernie-speed-8k"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("火山方舟", "火山引擎方舟", "https://ark.cn-beijing.volces.com/api/v3",
                    List.of("doubao-pro-32k", "doubao-lite-32k", "deepseek-r1"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("腾讯混元", "腾讯混元", "https://hunyuan.tencentcloudapi.com",
                    List.of("hunyuan-turbo", "hunyuan-large", "hunyuan-lite"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("SiliconFlow", "硅基流动", "https://api.siliconflow.cn/v1",
                    List.of("deepseek-ai/DeepSeek-V3", "deepseek-ai/DeepSeek-R1", "Qwen/Qwen2.5-72B-Instruct"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("OpenRouter", "OpenRouter", "https://openrouter.ai/api/v1",
                    List.of("openai/gpt-4o-mini", "anthropic/claude-3.5-sonnet", "google/gemini-flash-1.5"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("Anthropic", "Anthropic Claude", "https://api.anthropic.com/v1",
                    List.of("claude-3-5-sonnet-latest", "claude-3-5-haiku-latest", "claude-3-opus-latest"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("Google Gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta",
                    List.of("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash"), new BigDecimal("0.70"), 4096, 60),
            new ProviderPreset("Ollama", "Ollama 本地模型", "http://localhost:11434/v1",
                    List.of("llama3.1", "qwen2.5", "deepseek-r1"), new BigDecimal("0.70"), 4096, 120)
    );

    private final SysModelConfigService sysModelConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public SysModelConfigController(SysModelConfigService sysModelConfigService) {
        this.sysModelConfigService = sysModelConfigService;
    }

    @GetMapping("/list")
    public ResponseEntity<PageResponse<SysModelConfig>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                             @RequestParam(defaultValue = "10") int pageSize,
                                                             @RequestParam(required = false) String configName,
                                                             @RequestParam(required = false) String provider) {
        QueryWrapper<SysModelConfig> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(configName)) {
            wrapper.like("config_name", configName);
        }
        if (StringUtils.hasText(provider)) {
            wrapper.eq("provider", provider);
        }
        wrapper.orderByDesc("default_flag").orderByDesc("create_time");
        IPage<SysModelConfig> page = sysModelConfigService.page(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(this::maskApiKey);
        return ResponseEntity.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/active")
    public ResponseEntity<SysModelConfig> active() {
        SysModelConfig config = sysModelConfigService.getActiveDefault();
        maskApiKey(config);
        return ResponseEntity.success(config);
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderPreset>> providers() {
        return ResponseEntity.success(PROVIDER_PRESETS);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SysModelConfig> getById(@PathVariable Long id) {
        SysModelConfig config = sysModelConfigService.getById(id);
        maskApiKey(config);
        return ResponseEntity.success(config);
    }

    @PostMapping
    public ResponseEntity<SysModelConfig> create(@RequestBody SysModelConfig config) {
        sysModelConfigService.saveConfig(config);
        maskApiKey(config);
        return ResponseEntity.success("创建成功", config);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SysModelConfig> update(@PathVariable Long id, @RequestBody SysModelConfig config) {
        config.setId(id);
        sysModelConfigService.updateConfig(config);
        SysModelConfig updated = sysModelConfigService.getById(id);
        maskApiKey(updated);
        return ResponseEntity.success("更新成功", updated);
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<Void> setDefault(@PathVariable Long id) {
        sysModelConfigService.setDefault(id);
        return ResponseEntity.success("默认模型已更新", null);
    }

    @PostMapping("/test")
    public ResponseEntity<String> test(@RequestBody SysModelConfig config) {
        if (!StringUtils.hasText(config.getProvider()) || !StringUtils.hasText(config.getModelName())) {
            throw new IllegalArgumentException("请先填写模型厂商和模型名称");
        }
        if (!StringUtils.hasText(config.getApiKey()) || "******".equals(config.getApiKey())) {
            return ResponseEntity.success("基础配置校验通过，填写 API Key 后可测试连通性",
                    "provider=" + config.getProvider() + ", model=" + config.getModelName());
        }
        try {
            String baseUrl = StringUtils.hasText(config.getBaseUrl()) ? config.getBaseUrl() : "https://api.openai.com/v1";
            String endpoint = baseUrl.replaceAll("/+$", "") + "/chat/completions";
            Map<String, Object> payload = Map.of(
                    "model", config.getModelName(),
                    "temperature", 0,
                    "max_tokens", 32,
                    "messages", List.of(Map.of("role", "user", "content", "请只回复 OK"))
            );
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds() == null ? 30 : config.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ResponseEntity.error(response.statusCode(), "模型连接失败: " + response.body());
            }
            return ResponseEntity.success("模型连接成功", "provider=" + config.getProvider() + ", model=" + config.getModelName());
        } catch (Exception e) {
            return ResponseEntity.error("模型连接失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sysModelConfigService.removeById(id);
        return ResponseEntity.success(null);
    }

    private void maskApiKey(SysModelConfig config) {
        if (config != null && StringUtils.hasText(config.getApiKey())) {
            config.setApiKey("******");
        }
    }

    public record ProviderPreset(String name,
                                 String label,
                                 String baseUrl,
                                 List<String> models,
                                 BigDecimal temperature,
                                 Integer maxTokens,
                                 Integer timeoutSeconds) {
    }
}
