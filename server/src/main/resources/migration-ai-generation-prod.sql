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

ALTER TABLE sys_module ADD COLUMN design_json LONGTEXT COMMENT '模块设计JSON数据';

INSERT INTO sys_permission (permission_code, permission_name, module_name, description, project_id)
SELECT 'ai:generate', 'AI生成', 'AI生成', '分析、优化和生成模块草案', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'ai:generate');

INSERT INTO sys_permission (permission_code, permission_name, module_name, description, project_id)
SELECT 'ai:preview', 'AI预览', 'AI生成', '预览AI生成模块', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'ai:preview');

INSERT INTO sys_permission (permission_code, permission_name, module_name, description, project_id)
SELECT 'ai:publish', 'AI发布', 'AI生成', '保存、发布和部署AI生成模块', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'ai:publish');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id
FROM sys_permission p
WHERE p.permission_code IN ('ai:generate', 'ai:preview', 'ai:publish')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = 1 AND rp.permission_id = p.id
  );

ALTER TABLE sys_module DROP INDEX module_code;
CREATE UNIQUE INDEX uk_sys_module_code_project ON sys_module (module_code, project_id);
