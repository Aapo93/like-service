package com.aapo.common.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.SentinelWebInterceptor;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.SentinelWebPrefixInterceptor;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.config.SentinelWebMvcConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.Field;

@ConditionalOnClass({SentinelWebInterceptor.class})
@Configuration
public class SentinelConfig {

    @Autowired
    private SentinelWebMvcConfig sentinelWebMvcConfig;
    @Primary
    @Bean
    @ConditionalOnProperty(
            name = {"spring.cloud.sentinel.http-method-specify"},
            matchIfMissing = true
    )
    public SentinelWebPrefixInterceptor sentinelWebPrefixInterceptor() throws IllegalAccessException, NoSuchFieldException {
        //等待bugfix
        //https://github.com/alibaba/Sentinel/pull/3569
        SentinelWebPrefixInterceptor interceptor = new SentinelWebPrefixInterceptor();
        Field field = SentinelWebInterceptor.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(interceptor, sentinelWebMvcConfig);
        return interceptor;
    }
}
