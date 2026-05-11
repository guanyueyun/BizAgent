
package com.example.bizagent.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bizagent.modules.system.entity.SysMenu;
import com.example.bizagent.modules.system.mapper.SysMenuMapper;
import com.example.bizagent.modules.system.service.SysMenuService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> selectMenuByRoleId(Long roleId) {
        return baseMapper.selectMenuByRoleId(roleId);
    }
}
