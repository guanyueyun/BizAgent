
package com.example.bizagent.modules.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bizagent.common.PageResponse;
import com.example.bizagent.common.ResponseEntity;
import com.example.bizagent.modules.aiengine.dto.ModuleDesign;
import com.example.bizagent.modules.system.entity.SysModule;
import com.example.bizagent.modules.system.service.SysModuleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system/module")
public class SysModuleController {

    private final SysModuleService sysModuleService;
    private final ObjectMapper objectMapper;

    public SysModuleController(SysModuleService sysModuleService, ObjectMapper objectMapper) {
        this.sysModuleService = sysModuleService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/list")
    public ResponseEntity<PageResponse<SysModule>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        IPage<SysModule> page = sysModuleService.page(new Page<>(pageNum, pageSize));
        return ResponseEntity.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        SysModule module = sysModuleService.getById(id);
        if (module == null) {
            return ResponseEntity.error("模块不存在");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", module.getId());
        result.put("moduleName", module.getModuleName());
        result.put("moduleCode", module.getModuleCode());
        result.put("description", module.getDescription());
        result.put("frontPath", module.getFrontPath());
        result.put("backPath", module.getBackPath());
        result.put("status", module.getStatus());
        result.put("lifecycle", module.getLifecycle());
        result.put("createTime", module.getCreateTime());
        result.put("updateTime", module.getUpdateTime());
        
        if (module.getDesignJson() != null && !module.getDesignJson().isEmpty()) {
            try {
                ModuleDesign design = objectMapper.readValue(module.getDesignJson(), ModuleDesign.class);
                result.put("tables", design.getTables());
                result.put("pages", design.getPages());
                result.put("apis", design.getApis());
                result.put("permissions", design.getPermissions());
                result.put("menus", design.getMenus());
            } catch (JsonProcessingException e) {
                result.put("tables", null);
                result.put("pages", null);
                result.put("apis", null);
                result.put("permissions", null);
                result.put("menus", null);
            }
        } else {
            result.put("tables", null);
            result.put("pages", null);
            result.put("apis", null);
            result.put("permissions", null);
            result.put("menus", null);
        }
        
        return ResponseEntity.success(result);
    }

    @PostMapping
    public ResponseEntity<SysModule> create(@RequestBody SysModule module) {
        sysModuleService.save(module);
        return ResponseEntity.success("创建成功", module);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SysModule> update(@PathVariable Long id, @RequestBody SysModule module) {
        module.setId(id);
        sysModuleService.updateById(module);
        return ResponseEntity.success("更新成功", module);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sysModuleService.removeById(id);
        return ResponseEntity.success(null);
    }
}
