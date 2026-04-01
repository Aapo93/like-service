package com.aapo.api.config;

import com.aapo.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        //默认NONE
        return Logger.Level.NONE;
    }

    @Bean
    public RequestInterceptor useInterceptor() {
        return requestTemplate -> {
            Long userId = UserContext.getUser();
            if (userId != null) {
                requestTemplate.header("user-info", userId.toString());
            }
        };
    }
}