package com.aapo.common.domain;

import com.aapo.common.enums.ResourceType;
import lombok.Data;

@Data
public class LikeUserKey {

    public LikeUserKey(Long userId, ResourceType resourceType) {
        this.userId = userId;
        this.resourceType = resourceType;
    }

    private Long userId;
    private ResourceType resourceType;
}