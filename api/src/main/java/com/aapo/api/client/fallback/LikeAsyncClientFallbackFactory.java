package com.aapo.api.client.fallback;

import com.aapo.api.client.LikeAsynClient;
import com.aapo.common.enums.ResourceType;
import com.aapo.common.exception.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;

@Slf4j
public class LikeAsyncClientFallbackFactory implements FallbackFactory<LikeAsynClient> {
    @Override
    public LikeAsynClient create(Throwable cause) {
        return new LikeAsynClient() {
            @Override
            public Collection<Long> userLiked(ResourceType resourceType) {
                log.error("查询用户已点赞数据异常", cause);
                throw new BizIllegalException(cause);
            }

            @Override
            public Integer count(ResourceType resourceType, Long resourceId) {
                log.error("查询资源点赞数据异常", cause);
                throw new BizIllegalException(cause);
            }
        };
    }
}
