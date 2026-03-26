package com.aapo.gatewayservice.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {
    
    private final NacosConfigManager nacosConfigManager;
    
    private final RouteDefinitionWriter routeDefinitionWriter;
    
    private final String dataId = "gateway-routes.json";
    
    private final String group = "DEFAULT_GROUP";
    
    private final Set<String> routeIds = new HashSet<>();
    
    /**
     * bean初始化后执行方法
     */
    @PostConstruct
    public void initRoutConfigListener() throws NacosException {
        //1、项目启动时、先拉取一次配置，并添加配置监听器
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5 * 1000, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String s) {
                        //2、监听到配置变更，需要区更新路由表
                        updateConfigInfo(s);
                    }
                });
        //3、第一次读取到配置，需要更新到路由表
        updateConfigInfo(configInfo);
    }
    
    public void updateConfigInfo(String configInfo) {
        log.debug("监听到路由信息：{}", configInfo);
        //1、解析配置信息、转为RouteDefinition
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        //2、删除旧的路由表
        for (String routeId : routeIds) {
            routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
        }
        routeIds.clear();
        //3、更新路由表
        for (RouteDefinition routeDefinition : routeDefinitions) {
            routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
            //记录路由id，便于下次更新时删除
            routeIds.add(routeDefinition.getId());
        }
    }
}