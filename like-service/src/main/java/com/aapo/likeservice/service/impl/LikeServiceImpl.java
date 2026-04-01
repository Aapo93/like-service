package com.aapo.likeservice.service.impl;

import com.aapo.api.client.LikeAsynClient;
import com.aapo.common.enums.ResourceType;
import com.aapo.common.utils.CollUtils;
import com.aapo.common.utils.LikeUtils;
import com.aapo.common.utils.UserContext;
import com.aapo.likeservice.service.ILikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements ILikeService {

    private final LikeAsynClient likeAsynClient;

    private final LikeUtils likeUtils;

    @Override
    public void like(ResourceType resourceType, Long resourceId) {
        Assert.noNullElements(Arrays.asList(resourceType, resourceId), "点赞失败，参数错误");
        Long userId = UserContext.getUser();
        Assert.notNull(userId, "取消点赞失败，用户不存在");
        initLikeUserCache(userId, resourceType, resourceId);
        likeUtils.like(userId, resourceType, resourceId);
    }

    @Override
    public void unLike(ResourceType resourceType, Long resourceId) {
        Assert.noNullElements(Arrays.asList(resourceType, resourceId), "取消点赞失败，参数错误");
        Long userId = UserContext.getUser();
        Assert.notNull(userId, "取消点赞失败，用户不存在");
        initLikeUserCache(userId, resourceType, resourceId);
        likeUtils.unlike(userId, resourceType, resourceId);
    }

    /**
     * 初始化用户点赞缓存
     *
     * @param userId
     * @param resourceType
     */
    private void initLikeUserCache(Long userId, ResourceType resourceType, Long resourceId) {
        //判断用户点在记录是否存在缓存
        if (likeUtils.isExist(userId, resourceType)) {
            return;
        }
        //查询用户点赞记录并且进行缓存
        Collection<Long> resourceIds = likeAsynClient.userLiked(resourceType);
        likeUtils.saveLikeUserValue(userId, resourceType, resourceIds);
        //查询资源点赞数进行缓存
        Integer count = likeAsynClient.count(resourceType, resourceId);
        likeUtils.saveLikeCountValue(resourceType, resourceId, count.toString());
    }
}