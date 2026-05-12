package com.example.bizagent.modules.inspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bizagent.modules.inspection.entity.InspectionMain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface InspectionMainMapper extends BaseMapper<InspectionMain> {

    @Select("SELECT " +
            "COUNT(CASE WHEN status = 'pending' THEN 1 END) AS pendingCount, " +
            "COUNT(CASE WHEN status = 'completed' THEN 1 END) AS completedCount, " +
            "COUNT(CASE WHEN status = 'abnormal' THEN 1 END) AS abnormalCount, " +
            "COUNT(CASE WHEN rectification_status = 'pending' THEN 1 END) AS rectificationPendingCount, " +
            "COUNT(CASE WHEN rectification_status = 'rectified' THEN 1 END) AS rectificationCompletedCount " +
            "FROM biz_inspection_main " +
            "WHERE del_flag = 0 AND project_id = #{projectId} " +
            "AND inspection_time >= #{startTime} AND inspection_time <= #{endTime}")
    Map<String, Object> selectStatistics(@Param("projectId") Long projectId,
                                         @Param("startTime") String startTime,
                                         @Param("endTime") String endTime);
}