CREATE TABLE IF NOT EXISTS biz_approval_main (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    biz_no VARCHAR(50) NOT NULL COMMENT '业务编号',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    owner_name VARCHAR(100) COMMENT '负责人',
    status VARCHAR(30) NOT NULL DEFAULT 'draft' COMMENT '状态',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标识',
    project_id BIGINT NOT NULL DEFAULT 1 COMMENT '项目ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='approval主表';

CREATE TABLE IF NOT EXISTS biz_approval_approval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    biz_id BIGINT NOT NULL COMMENT '业务数据ID',
    node_name VARCHAR(100) NOT NULL COMMENT '审批节点',
    approver VARCHAR(100) COMMENT '审批人',
    approval_status VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT '审批状态',
    approval_comment VARCHAR(500) COMMENT '审批意见',
    approval_time DATETIME COMMENT '审批时间',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标识',
    project_id BIGINT NOT NULL DEFAULT 1 COMMENT '项目ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录';

