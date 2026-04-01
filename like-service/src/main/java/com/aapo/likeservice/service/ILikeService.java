package com.aapo.likeservice.service;

import com.aapo.common.enums.ResourceType;

public interface ILikeService {

    /**
     * 点赞
     *
     * @param resourceType
     * @param resourceId
     */
    void like(ResourceType resourceType, Long resourceId);

    /**
     * 取消点赞
     *
     * @param resourceType
     * @param resourceId
     */
    void unLike(ResourceType resourceType, Long resourceId);
}