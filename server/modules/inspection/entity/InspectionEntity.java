package com.example.bizagent.modules.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_inspection_main")
public class InspectionMain {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("device_name")
    private String deviceName;

    @TableField("device_location")
    private String deviceLocation;

    @TableField("plan_name")
    private String planName;

    @TableField("inspector")
    private String inspector;

    @TableField("inspection_time")
    private String inspectionTime;

    @TableField("status")
    private String status;

    @TableField("exception_desc")
    private String exceptionDesc;

    @TableField("rectification_status")
    private String rectificationStatus;

    @TableField("rectification_measure")
    private String rectificationMeasure;

    @TableField("rectification_person")
    private String rectificationPerson;

    @TableField("rectification_time")
    private String rectificationTime;

    @TableField("del_flag")
    private Integer delFlag;

    @TableField("project_id")
    private Long projectId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;
}