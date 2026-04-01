package com.aapo.likeasynservice.service;

import com.aapo.likeasynservice.domain.po.LikeStatistics;
import com.aapo.likeasynservice.domain.query.ResourcePairs;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ILikeStatisticsService extends IService<LikeStatistics> {

    /**
     * 根据资源类型和资源id查询点赞统计数据
     * @param pairList
     * @return
     */
    List<LikeStatistics> batchQueryByPairs(List<ResourcePairs> pairList);
}
