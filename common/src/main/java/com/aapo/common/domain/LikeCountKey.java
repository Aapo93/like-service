package com.aapo.common.domain;

import com.aapo.common.enums.ResourceType;
import lombok.Data;

@Data
public class LikeCountKey {

    public LikeCountKey(ResourceType resourceType, Long resourceId) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    private ResourceType resourceType;
    private Long resourceId;
}