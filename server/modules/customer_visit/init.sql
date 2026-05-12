CREATE TABLE IF NOT EXISTS biz_customer_visit_main (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    visit_time DATETIME NOT NULL COMMENT '回访时间',
    visit_method VARCHAR(20) NOT NULL DEFAULT '电话' COMMENT '回访方式',
    visit_result TEXT COMMENT '回访结果',
    follower VARCHAR(50) NOT NULL COMMENT '跟进人',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-待处理，completed-已完成，cancelled-已取消',
    create_by VARCHAR(64) DEFAULT '' COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT '' COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标记',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    KEY idx_project_id (project_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户回访主表';