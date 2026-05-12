package com.example.bizagent.modules.customer_visit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bizagent.modules.customer_visit.entity.CustomerVisitMain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface CustomerVisitMainMapper extends BaseMapper<CustomerVisitMain> {

    @Select("SELECT visit_method AS method, COUNT(*) AS count FROM biz_customer_visit_main WHERE project_id = #{projectId} AND del_flag = 0 GROUP BY visit_method")
    List<Map<String, Object>> countByVisitMethod(@Param("projectId") Long projectId);

    @Select("SELECT status, COUNT(*) AS count FROM biz_customer_visit_main WHERE project_id = #{projectId} AND del_flag = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("projectId") Long projectId);

    @Select("SELECT DATE_FORMAT(visit_time, '%Y-%m') AS month, COUNT(*) AS count FROM biz_customer_visit_main WHERE project_id = #{projectId} AND del_flag = 0 GROUP BY month ORDER BY month")
    List<Map<String, Object>> countByMonth(@Param("projectId") Long projectId);
}