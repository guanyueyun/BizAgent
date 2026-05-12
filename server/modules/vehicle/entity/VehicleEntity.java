package com.example.bizagent.modules.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("biz_vehicle_main")
public class VehicleMain implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("plate_number")
    private String plateNumber;

    @TableField("brand")
    private String brand;

    @TableField("model")
    private String model;

    @TableField("color")
    private String color;

    @TableField("purchase_date")
    private String purchaseDate;

    @TableField("mileage")
    private String mileage;

    @TableField("status")
    private String status;

    @TableField("approval_status")
    private String approvalStatus;

    @TableField("driver")
    private String driver;

    @TableField("remark")
    private String remark;

    @TableField("project_id")
    private Long projectId;

    @TableField("del_flag")
    private Integer delFlag;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}