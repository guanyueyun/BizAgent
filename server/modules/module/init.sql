CREATE TABLE IF NOT EXISTS biz_module_main (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    biz_no VARCHAR(50) NOT NULL COMMENT '业务编号',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    owner_name VARCHAR(100) COMMENT '负责人',
    contact_phone VARCHAR(30) COMMENT '联系电话',
    biz_date DATE COMMENT '业务日期',
    biz_time DATETIME COMMENT '业务时间',
    status VARCHAR(30) NOT NULL DEFAULT 'draft' COMMENT '状态',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标识',
    project_id BIGINT NOT NULL DEFAULT 1 COMMENT '项目ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='module主表';

