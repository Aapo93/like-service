package com.aapo.likeasynservice.service.impl;

import com.aapo.common.utils.CollUtils;
import com.aapo.likeasynservice.domain.po.LikeStatistics;
import com.aapo.likeasynservice.domain.query.ResourcePairs;
import com.aapo.likeasynservice.mapper.LikeStatisticsMapper;
import com.aapo.likeasynservice.service.ILikeStatisticsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeStatisticsServiceImpl extends ServiceImpl<LikeStatisticsMapper, LikeStatistics> implements ILikeStatisticsService {

    private final LikeStatisticsMapper likeStatisticsMapper;

    @Override
    public List<LikeStatistics> batchQueryByPairs(List<ResourcePairs> pairList) {
        if (CollUtils.isEmpty(pairList)) {
            return Collections.emptyList();
        }
        return likeStatisticsMapper.batchQueryByPairs(pairList);
    }
}