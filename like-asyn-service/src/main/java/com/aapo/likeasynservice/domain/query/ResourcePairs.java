package com.aapo.likeasynservice.domain.query;

import com.aapo.common.enums.ResourceType;
import lombok.Data;

@Data
public class ResourcePairs {

    public ResourcePairs(ResourceType resourceType, Long resourceId) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    private ResourceType resourceType;
    private Long resourceId;
}