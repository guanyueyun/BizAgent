
package com.example.bizagent.modules.modulecontainer.service;

import com.example.bizagent.modules.system.entity.SysModule;

public interface ModuleLoaderService {

    void loadModule(Long moduleId);

    void unloadModule(Long moduleId);

    void reloadModule(Long moduleId);

    boolean isModuleLoaded(Long moduleId);

    void mountPermissions(Long moduleId);

    void mountMenus(Long moduleId);

    void mountRoutes(Long moduleId);

    void mountToProject(Long moduleId, Long projectId);
}
