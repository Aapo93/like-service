package com.aapo.likeasynservice.conroller;

import com.aapo.common.enums.ResourceType;
import com.aapo.common.utils.CollUtils;
import com.aapo.common.utils.UserContext;
import com.aapo.likeasynservice.domain.po.LikeRecord;
import com.aapo.likeasynservice.domain.po.LikeStatistics;
import com.aapo.likeasynservice.service.ILikeRecordService;
import com.aapo.likeasynservice.service.ILikeService;
import com.aapo.likeasynservice.service.ILikeStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/like")
@RequiredArgsConstructor
public class LikeController {

    private final ILikeRecordService likeRecordService;

    private final ILikeStatisticsService likeStatisticsService;

    private final ILikeService likeService;

    /**
     * 用户已点赞
     * @param resourceType
     * @return
     */
    @GetMapping("/user_liked")
    public Collection<Long> userLiked(ResourceType resourceType) {
        Collection<Long> resourceIds = new HashSet<>();
        List<LikeRecord> list = likeRecordService.lambdaQuery()
                .select(LikeRecord::getResourceId)
                .eq(LikeRecord::getUserId, UserContext.getUser())
                .eq(LikeRecord::getResourceType, resourceType).list();
        if (CollUtils.isNotEmpty(list)) {
            resourceIds.addAll(list.stream().map(v -> v.getResourceId()).collect(Collectors.toSet()));
        }
        return resourceIds;
    }

    /**
     * 资源点赞数
     * @param resourceType
     * @param resourceId
     * @return
     */
    @GetMapping("/count")
    public Integer count(ResourceType resourceType, Long resourceId) {
        LikeStatistics likeStatistics = likeStatisticsService.lambdaQuery()
                .eq(LikeStatistics::getResourceType, resourceType)
                .eq(LikeStatistics::getResourceId, resourceId).one();
        return likeStatistics != null ? likeStatistics.getCount() : 0;
    }

    @GetMapping("/test")
    public void test() {
        likeService.statistics();
    }
}
