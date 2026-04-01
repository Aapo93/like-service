package com.aapo.api.client;

import com.aapo.api.client.fallback.LikeAsyncClientFallbackFactory;
import com.aapo.common.enums.ResourceType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

@FeignClient(value = "like-asyn-service", fallbackFactory = LikeAsyncClientFallbackFactory.class)
public interface LikeAsynClient {

    /**
     * 用户已点赞数据
     *
     * @param resourceType
     * @return
     */
    @GetMapping("/internal/like/user_liked")
    Collection<Long> userLiked(@RequestParam("resourceType") ResourceType resourceType);

    /**
     * 资源点赞数
     * @param resourceType
     * @param resourceId
     * @return
     */
    @GetMapping("/internal/like/count")
    Integer count(@RequestParam("resourceType")ResourceType resourceType, @RequestParam("resourceId")Long resourceId);
}