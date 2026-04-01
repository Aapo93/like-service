package com.aapo.likeservice.controller;

import com.aapo.api.client.LikeAsynClient;
import com.aapo.common.enums.ResourceType;
import com.aapo.likeservice.service.ILikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController {

    private final ILikeService iLikeService;

    private final LikeAsynClient likeAsynClient;


    /**
     * 点赞
     * @param resourceType
     * @param resourceId
     */
    @PostMapping
    public void like(@RequestParam("resourceType") ResourceType resourceType, @RequestParam("resourceId") Long resourceId) {
        iLikeService.like(resourceType, resourceId);
    }

    /**
     * 取消点赞
     * @param resourceType
     * @param resourceId
     */
    @DeleteMapping
    public void unlike(@RequestParam("resourceType") ResourceType resourceType, @RequestParam("resourceId") Long resourceId) {
        iLikeService.unLike(resourceType, resourceId);
    }
}