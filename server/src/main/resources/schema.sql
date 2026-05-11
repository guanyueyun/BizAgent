CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) COMMENT '密码',
    real_name VARCHAR(100) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status INT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标志 0正常 1删除',
    project_id BIGINT DEFAULT 0 COMMENT '项目ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(500) COMMENT '描述',
    status INT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标志 0正常 1删除',
    project_id BIGINT DEFAULT 0 COMMENT '项目ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    path VARCHAR(255) COMMENT '路由路径',
    component VARCHAR(255) COMMENT '组件路径',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    menu_type INT DEFAULT 1 COMMENT '菜单类型 1目录 2菜单 3按钮',
    permission VARCHAR(100) COMMENT '权限标识',
    visible INT DEFAULT 1 COMMENT '是否可见 0隐藏 1显示',
    status INT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标志 0正常 1删除',
    project_id BIGINT DEFAULT 0 COMMENT '项目ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    module_name VARCHAR(100) COMMENT '所属模块',
    description VARCHAR(500) COMMENT '描述',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标志 0正常 1删除',
    project_id BIGINT DEFAULT 0 COMMENT '项目ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS sys_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    project_name VARCHAR(100) NOT NULL COMMENT '项目名称',
    project_code VARCHAR(50) NOT NULL UNIQUE COMMENT '项目编码',
    description VARCHAR(500) COMMENT '描述',
    status INT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标志 0正常 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

CREATE TABLE IF NOT EXISTS sys_module (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    module_name VARCHAR(100) NOT NULL COMMENT '模块名称',
    module_code VARCHAR(50) NOT NULL COMMENT '模块编码',
    description VARCHAR(500) COMMENT '描述',
    front_path VARCHAR(255) COMMENT '前端模块路径',
    back_path VARCHAR(255) COMMENT '后端模块路径',
    design_json LONGTEXT COMMENT '模块设计JSON数据',
    status INT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    lifecycle INT DEFAULT 0 COMMENT '生命周期 0开发中 1测试中 2已发布 3已停用 4已卸载',
    version VARCHAR(20) DEFAULT '1.0.0' COMMENT '版本',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标志 0正常 1删除',
    project_id BIGINT DEFAULT 0 COMMENT '项目ID',
    UNIQUE KEY uk_sys_module_code_project (module_code, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    module_code VARCHAR(100) COMMENT '模块编码',
    user_id BIGINT COMMENT '操作用户',
    project_id BIGINT DEFAULT 1 COMMENT '项目ID',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    message VARCHAR(1000) COMMENT '日志内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

CREATE TABLE IF NOT EXISTS sys_model_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    provider VARCHAR(50) NOT NULL COMMENT '模型厂商',
    base_url VARCHAR(500) COMMENT '接口地址',
    api_key VARCHAR(500) COMMENT 'API Key',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    temperature DECIMAL(4,2) DEFAULT 0.70 COMMENT '温度',
    max_tokens INT DEFAULT 4096 COMMENT '最大Token数',
    timeout_seconds INT DEFAULT 60 COMMENT '超时时间秒',
    default_flag INT DEFAULT 0 COMMENT '是否默认 0否 1是',
    status INT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    description VARCHAR(500) COMMENT '描述',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标志 0正常 1删除',
    project_id BIGINT DEFAULT 0 COMMENT '项目ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型配置表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

INSERT IGNORE INTO sys_project (id, project_name, project_code, description, status) VALUES (1, '默认项目', 'default', '系统默认项目', 1);
INSERT IGNORE INTO sys_role (id, role_name, role_code, description, status) VALUES (1, '超级管理员', 'admin', '系统超级管理员', 1);
INSERT IGNORE INTO sys_user (id, username, password, real_name, email, status, project_id) VALUES (1, 'admin', '$2a$10$p4K.xYwo/0/m6qQ7PVgbge33QWFRe0VMGExsCQjuE8TI1BwADbMxO', '管理员', 'admin@example.com', 1, 1);
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);

INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (1, 'user:list', '用户查看', '系统管理', '查看用户列表');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (2, 'user:add', '用户新增', '系统管理', '新增用户');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (3, 'user:edit', '用户编辑', '系统管理', '编辑用户');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (4, 'user:delete', '用户删除', '系统管理', '删除用户');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (5, 'role:list', '角色查看', '系统管理', '查看角色列表');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (6, 'role:add', '角色新增', '系统管理', '新增角色');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (7, 'role:edit', '角色编辑', '系统管理', '编辑角色');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (8, 'role:delete', '角色删除', '系统管理', '删除角色');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (9, 'permission:list', '权限查看', '系统管理', '查看权限列表');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (10, 'permission:add', '权限新增', '系统管理', '新增权限');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (11, 'permission:edit', '权限编辑', '系统管理', '编辑权限');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (12, 'permission:delete', '权限删除', '系统管理', '删除权限');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (13, 'menu:list', '菜单查看', '系统管理', '查看菜单列表');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (14, 'menu:add', '菜单新增', '系统管理', '新增菜单');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (15, 'menu:edit', '菜单编辑', '系统管理', '编辑菜单');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (16, 'menu:delete', '菜单删除', '系统管理', '删除菜单');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (17, 'module:list', '模块查看', '模块管理', '查看模块列表');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (18, 'module:add', '模块新增', '模块管理', '新增模块');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (19, 'module:edit', '模块编辑', '模块管理', '编辑模块');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (20, 'module:delete', '模块删除', '模块管理', '删除模块');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (21, 'module:deploy', '模块部署', '模块管理', '部署模块');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (22, 'project:list', '项目查看', '项目管理', '查看项目列表');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (23, 'project:add', '项目新增', '项目管理', '新增项目');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (24, 'project:edit', '项目编辑', '项目管理', '编辑项目');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (25, 'project:delete', '项目删除', '项目管理', '删除项目');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (26, 'model-config:list', '模型配置查看', '系统管理', '查看模型配置');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (27, 'model-config:add', '模型配置新增', '系统管理', '新增模型配置');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (28, 'model-config:edit', '模型配置编辑', '系统管理', '编辑模型配置');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (29, 'model-config:delete', '模型配置删除', '系统管理', '删除模型配置');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (30, 'ai:generate', 'AI生成', 'AI生成', '分析、优化和生成模块草案');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (31, 'ai:preview', 'AI预览', 'AI生成', '预览AI生成模块');
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, module_name, description) VALUES (32, 'ai:publish', 'AI发布', 'AI生成', '保存、发布和部署AI生成模块');

INSERT IGNORE INTO sys_model_config (id, config_name, provider, base_url, model_name, temperature, max_tokens, timeout_seconds, default_flag, status, description, project_id)
VALUES (1, '默认模型', 'OpenAI-Compatible', 'https://api.openai.com/v1', 'gpt-4o-mini', 0.70, 4096, 60, 1, 1, '默认兼容 OpenAI Chat Completions 的模型配置', 1);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 1);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 2);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 3);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 4);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 5);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 6);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 7);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 8);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 9);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 10);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 11);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 12);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 13);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 14);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 15);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 16);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 17);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 18);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 19);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 20);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 21);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 22);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 23);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 24);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 25);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 26);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 27);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 28);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 29);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 30);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 31);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 32);
