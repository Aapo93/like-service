package com.aapo.common.domain;

import com.aapo.common.enums.ResourceType;
import lombok.Data;

/**
 * 点赞操作
 */
@Data
public class LikeOperate {
    public LikeOperate(){}

    public LikeOperate(Long userId, ResourceType resourceType, Long resourceId, Action action) {
        this.userId = userId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.action = action;
    }

    private Long userId;
    private ResourceType resourceType;
    private Long resourceId;
    private Action action;

    public enum Action {
        like,
        unlike
    }
}