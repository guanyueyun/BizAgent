package com.example.bizagent.modules.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_approval_main")
public class ApprovalEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String biz_no; // 业务编号
    private String name; // 名称
    private String owner_name; // 负责人
    private String status; // 状态
    private String remark; // 备注
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer delFlag;
    private Long projectId;
}
