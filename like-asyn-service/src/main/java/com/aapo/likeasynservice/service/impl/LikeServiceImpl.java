package com.aapo.likeasynservice.service.impl;

import com.aapo.common.domain.LikeCountKey;
import com.aapo.common.domain.LikeOperate;
import com.aapo.common.domain.LikeUserKey;
import com.aapo.common.enums.ResourceType;
import com.aapo.common.utils.BeanUtils;
import com.aapo.common.utils.CollUtils;
import com.aapo.common.utils.LikeUtils;
import com.aapo.likeasynservice.domain.po.LikeRecord;
import com.aapo.likeasynservice.domain.po.LikeStatistics;
import com.aapo.likeasynservice.domain.query.ResourcePairs;
import com.aapo.likeasynservice.service.ILikeRecordService;
import com.aapo.likeasynservice.service.ILikeService;
import com.aapo.likeasynservice.service.ILikeStatisticsService;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements ILikeService {

    private final LikeUtils likeUtils;

    private final ILikeRecordService likeRecordService;
    private final ILikeStatisticsService likeStatisticsService;

    @Override
    @Transactional
    public void statistics() {
        //从待同步队列获取100条数据
        List<LikeOperate> likeOperate = likeUtils.getLikeOperate(100);
        if (CollUtils.isEmpty(likeOperate)) {
            return;
        }
        //{"userId":19,"resourceType":"ARTICLE","resourceId":2,"action":"like"}
        //{"userId":20,"resourceType":"ARTICLE","resourceId":2,"action":"like"}
        //{"userId":30,"resourceType":"ARTICLE","resourceId":2,"action":"unlike"}

        //准备数据
        Set<Long> userIds = new HashSet<>();
        Set<String> likeUserKeys = new HashSet<>();
        Set<String> likeCountKeys = new HashSet<>();
        likeOperate.forEach(v -> {
            userIds.add(v.getUserId());
            likeUserKeys.add(likeUtils.getLikeUserKey(v.getUserId(), v.getResourceType()));
            likeCountKeys.add(likeUtils.getLikeCountKey(v.getResourceType(), v.getResourceId()));

        });
        //同步用户点赞记录
        //1、查询用户点赞记录
        Map<String, Set<Long>> likeUserValues = likeUtils.batchGetLikeUserValue(likeUserKeys);
        //2、筛选点赞记录，新增或删除
        List<LikeRecord> likeRecordList = likeRecordService.lambdaQuery().in(LikeRecord::getUserId, userIds).list();
        //删除：redis没值、数据库有值
        List<LikeRecord> removeLikeRecordList = likeRecordList.stream().filter(v -> {
            String key = likeUtils.getLikeUserKey(v.getUserId(), v.getResourceType());
            Set<Long> value = likeUserValues.get(key);
            return value == null || !value.contains(v.getResourceId());
        }).toList();
        //新增：redis有值、数据库没值
        List<LikeRecord> addLikeRecordList = new ArrayList<>();
        likeRecordList.forEach(v -> {
            String key = likeUtils.getLikeUserKey(v.getUserId(), v.getResourceType());
            Set<Long> value = likeUserValues.get(key);
            value.remove(v.getResourceId());
        });
        for (Map.Entry<String, Set<Long>> entry : likeUserValues.entrySet()) {
            LikeUserKey likeUserKey = likeUtils.getLikeUserKey(entry.getKey());
            for (Long resourceId : entry.getValue()) {
                LikeRecord likeRecord = new LikeRecord();
                likeRecord.setUserId(likeUserKey.getUserId());
                likeRecord.setResourceType(likeUserKey.getResourceType());
                likeRecord.setResourceId(resourceId);
                addLikeRecordList.add(likeRecord);
            }
        }
        if (CollUtils.isNotEmpty(removeLikeRecordList)) {
            likeRecordService.removeBatchByIds(removeLikeRecordList);
        }
        if (CollUtils.isNotEmpty(addLikeRecordList)) {
            likeRecordService.saveBatch(addLikeRecordList);
        }
        //同步资源点赞数
        //1、查询资源点赞数
        Map<String, Integer> likeCountValues = likeUtils.batchGetLikeCountValue(likeCountKeys);
        //2、筛选点赞记录，新增或删除
        //方式一：拼接or条件，性能问题
        /*LambdaQueryChainWrapper<LikeStatistics> wrapper = likeStatisticsService.lambdaQuery();
        boolean orFlag = true;
        for (Map.Entry<String, Integer> entry : likeCountValues.entrySet()) {
            LikeCountKey likeCountKey = likeUtils.getLikeCountKey(entry.getKey());
            if (orFlag) {
                orFlag = !orFlag;
                wrapper.eq(LikeStatistics::getResourceType, likeCountKey.getResourceType())
                        .eq(LikeStatistics::getResourceId, likeCountKey.getResourceId());
            } else {
                wrapper.or(o ->
                        o.eq(LikeStatistics::getResourceType, likeCountKey.getResourceType())
                                .eq(LikeStatistics::getResourceId, likeCountKey.getResourceId()));
            }
        }*/
        //方式二：自定义mapper接口，where (resourceType, resourceId) in ((1,1),(2,2),...)，mysql8语法支持
        List<ResourcePairs> pairList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : likeCountValues.entrySet()) {
            LikeCountKey likeCountKey = likeUtils.getLikeCountKey(entry.getKey());
            pairList.add(BeanUtils.copyBean(likeCountKey, ResourcePairs.class));
        }
        //修改：redis有数据，数据库有数据
        List<LikeStatistics> list = likeStatisticsService.batchQueryByPairs(pairList);
        list.forEach(v -> {
            String key = likeUtils.getLikeCountKey(v.getResourceType(), v.getResourceId());
            Integer count = likeCountValues.get(key);
            if (count != null) {
                v.setCount(count);
                likeCountValues.remove(key);
            }
        });
        //新增：redis有数据，数据库没数据
        for (Map.Entry<String, Integer> entry : likeCountValues.entrySet()) {
            LikeCountKey likeCountKey = likeUtils.getLikeCountKey(entry.getKey());
            LikeStatistics likeStatistics = new LikeStatistics();
            likeStatistics.setResourceType(likeCountKey.getResourceType());
            likeStatistics.setResourceId(likeCountKey.getResourceId());
            likeStatistics.setCount(entry.getValue());
            list.add(likeStatistics);
        }
        likeStatisticsService.saveOrUpdateBatch(list);
    }
}