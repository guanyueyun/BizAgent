package com.example.bizagent.modules.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_customer_main")
public class CustomerMain {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String customerName;

    private String contactPerson;

    private String contactPhone;

    private String industry;

    private String source;

    private String followUpPerson;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private Long projectId;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}