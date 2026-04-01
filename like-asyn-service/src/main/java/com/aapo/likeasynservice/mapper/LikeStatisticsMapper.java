package com.aapo.likeasynservice.mapper;

import com.aapo.likeasynservice.domain.po.LikeStatistics;
import com.aapo.likeasynservice.domain.query.ResourcePairs;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LikeStatisticsMapper extends BaseMapper<LikeStatistics> {

    /**
     * 根据resourcePairs批量查询点赞统计数据
     *
     * @param pairList
     * @return
     */
    List<LikeStatistics> batchQueryByPairs(@Param("pairList") List<ResourcePairs> pairList);
}