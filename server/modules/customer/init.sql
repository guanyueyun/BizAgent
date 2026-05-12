CREATE TABLE IF NOT EXISTS biz_customer_main (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    contact_person VARCHAR(100) COMMENT '联系人',
    contact_phone VARCHAR(100) COMMENT '联系电话',
    industry VARCHAR(100) COMMENT '所属行业',
    source VARCHAR(100) COMMENT '客户来源',
    follow_up_person VARCHAR(100) COMMENT '跟进人',
    status VARCHAR(100) NOT NULL DEFAULT 'potential' COMMENT '跟进状态: potential-潜在, contacted-已联系, negotiating-洽谈中, converted-已成交, lost-已流失',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标记',
    project_id BIGINT COMMENT '项目ID',
    KEY idx_project_id (project_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户主表';