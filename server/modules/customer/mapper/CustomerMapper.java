package com.example.bizagent.modules.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bizagent.modules.customer.entity.CustomerMain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface CustomerMainMapper extends BaseMapper<CustomerMain> {

    @Select("SELECT status, COUNT(*) AS count FROM biz_customer_main WHERE project_id = #{projectId} AND del_flag = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("projectId") Long projectId);

    @Select("SELECT source, COUNT(*) AS count FROM biz_customer_main WHERE project_id = #{projectId} AND del_flag = 0 GROUP BY source")
    List<Map<String, Object>> countBySource(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM biz_customer_main WHERE project_id = #{projectId} AND del_flag = 0 AND DATE(create_time) = CURDATE()")
    Long countTodayAdded(@Param("projectId") Long projectId);
}