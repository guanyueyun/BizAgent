package com.example.bizagent.modules.customer_visit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_customer_visit_main")
public class CustomerVisitMain {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String customerName;

    private LocalDateTime visitTime;

    private String visitMethod;

    private String visitResult;

    private String follower;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer delFlag;
}