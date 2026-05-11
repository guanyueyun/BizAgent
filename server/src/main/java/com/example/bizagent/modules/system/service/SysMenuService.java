
package com.example.bizagent.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.bizagent.modules.system.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    List<SysMenu> selectMenuByRoleId(Long roleId);
}
