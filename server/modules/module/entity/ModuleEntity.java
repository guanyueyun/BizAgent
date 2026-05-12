package com.example.bizagent.modules.module.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_module_main")
public class ModuleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String biz_no; // 业务编号
    private String name; // 名称
    private String owner_name; // 负责人
    private String contact_phone; // 联系电话
    private java.time.LocalDateTime biz_time; // 业务时间
    private String status; // 状态
    private String remark; // 备注
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer delFlag;
    private Long projectId;
}
