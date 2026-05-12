CREATE TABLE IF NOT EXISTS biz_contract_main (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    biz_no VARCHAR(50) NOT NULL COMMENT '业务编号',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    owner_name VARCHAR(100) COMMENT '负责人',
    customer_name VARCHAR(100) COMMENT '客户名称',
    amount DECIMAL(18,2) COMMENT '金额',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='contract主表';

CREATE TABLE IF NOT EXISTS biz_contract_approval (
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

CREATE TABLE IF NOT EXISTS biz_contract_stat_day (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    stat_date DATE NOT NULL COMMENT '统计日期',
    total_count INT NOT NULL DEFAULT 0 COMMENT '总数量',
    done_count INT NOT NULL DEFAULT 0 COMMENT '完成数量',
    exception_count INT NOT NULL DEFAULT 0 COMMENT '异常数量',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标识',
    project_id BIGINT NOT NULL DEFAULT 1 COMMENT '项目ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日统计';

CREATE TABLE IF NOT EXISTS biz_contract_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    biz_id BIGINT NOT NULL COMMENT '业务数据ID',
    receiver VARCHAR(100) COMMENT '接收人',
    message_title VARCHAR(200) NOT NULL COMMENT '消息标题',
    message_content VARCHAR(500) COMMENT '消息内容',
    read_status VARCHAR(30) NOT NULL DEFAULT 'unread' COMMENT '阅读状态',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标识',
    project_id BIGINT NOT NULL DEFAULT 1 COMMENT '项目ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息提醒';

