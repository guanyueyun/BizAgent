package com.example.bizagent.modules.equipment_inspection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_equipment_inspection_main")
public class EquipmentInspectionMain {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String deviceName;

    private String deviceLocation;

    private String inspectionPlan;

    private String inspector;

    private LocalDateTime inspectionTime;

    private String exceptionDesc;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}