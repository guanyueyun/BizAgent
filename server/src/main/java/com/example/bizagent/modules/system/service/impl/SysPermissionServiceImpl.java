
package com.example.bizagent.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bizagent.modules.system.entity.SysPermission;
import com.example.bizagent.modules.system.mapper.SysPermissionMapper;
import com.example.bizagent.modules.system.service.SysPermissionService;
import org.springframework.stereotype.Service;

@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {
}
