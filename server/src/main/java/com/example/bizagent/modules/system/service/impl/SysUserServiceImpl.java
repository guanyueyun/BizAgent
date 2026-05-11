
package com.example.bizagent.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bizagent.modules.system.entity.SysUser;
import com.example.bizagent.modules.system.mapper.SysUserMapper;
import com.example.bizagent.modules.system.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
}
