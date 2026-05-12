CREATE TABLE IF NOT EXISTS biz_equipment_inspection_main (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    device_name VARCHAR(100) NOT NULL COMMENT '设备名称',
    device_location VARCHAR(200) NOT NULL COMMENT '设备位置',
    inspection_plan VARCHAR(100) DEFAULT NULL COMMENT '巡检计划',
    inspector VARCHAR(50) NOT NULL COMMENT '巡检人',
    inspection_time DATETIME NOT NULL COMMENT '巡检时间',
    exception_desc TEXT DEFAULT NULL COMMENT '异常描述',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-待巡检，completed-已完成，exception-异常',
    create_by BIGINT NOT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    KEY idx_project_id (project_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备巡检主表';