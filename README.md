# 点赞系统技术方案

## 1. 系统概述

### 1.1 业务需求
- 支持用户对各种资源（文章、视频、评论等）进行点赞/取消点赞
- 支持实时查询点赞数
- 支持查询用户点赞状态
- 高并发场景下的系统稳定性
- 保证数据最终一致性

### 1.2 技术栈
- **JDK 21**：使用虚拟线程提升并发性能
- **Spring Boot 3.x**：现代化Java应用框架
- **RabbitMQ**：消息队列，用于流量削峰和异步处理
- **MyBatis Plus**：持久层框架，提升开发效率
- **Redis**：缓存层，存储热点数据和计数器
- **MySQL**：持久化存储，保证数据可靠性

---

## 2. 系统架构设计

### 2.1 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
│                    (Web/Mobile/API)                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     API网关层                               │
│                (限流/鉴权/路由)                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                  点赞服务层 (JDK 21 + Spring Boot)          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            实时处理模块                               │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │  点赞接口   │  │  取消点赞   │  │  状态查询   │  │  │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  │  │
│  │         │                 │                 │        │  │
│  │         ▼                 ▼                 ▼        │  │
│  │  ┌──────────────────────────────────────────────┐  │  │
│  │  │         Redis 缓存层                          │  │  │
│  │  │  - 用户点赞状态 (Set)                         │  │  │
│  │  │  - 资源点赞计数 (String)                      │  │  │
│  │  │  - 待同步队列 (List)                         │  │  │
│  │  └──────────────────────────────────────────────┘  │  │
│  └──────────────────────────┬───────────────────────────┘  │
│                             │                              │
│                             ▼                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            异步处理模块                               │  │
│  │  ┌──────────────────────────────────────────────┐  │  │
│  │  │     RabbitMQ 消息队列                        │  │  │
│  │  │  - like.queue (点赞事件队列)                  │  │  │
│  │  │  - sync.queue (数据同步队列)                  │  │  │
│  │  └──────────────────────────────────────────────┘  │  │
│  └──────────────────────────┬───────────────────────────┘  │
└─────────────────────────────┼───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   消费者服务                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │     定时任务消费者                                   │  │
│  │     - 批量读取Redis队列数据                          │  │
│  │     - 聚合处理                                      │  │
│  │     - 批量写入MySQL                                 │  │
│  └──────────────────────────┬───────────────────────────┘  │
└─────────────────────────────┼───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   数据持久层                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              MySQL 数据库                            │  │
│  │  - like_record (点赞记录表)                         │  │
│  │  - like_statistics (点赞统计表)                     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 数据流转图

```
用户请求
    │
    ├─► [1] 点赞/取消点赞请求
    │       │
    │       ├─► 检查参数 & 用户权限
    │       │
    │       ├─► 写入Redis (原子操作)
    │       │   ├─► 用户点赞状态: SADD/SREM
    │       │   ├─► 资源计数: INCR/DECR
    │       │   └─► 待同步队列: LPUSH
    │       │
    │       ├─► 发送消息到RabbitMQ
    │       │   └─► 异步处理通知
    │       │
    │       └─► 返回结果给用户 (O(1))
    │
    ├─► [2] 查询点赞数
    │       │
    │       └─► 直接从Redis读取 (O(1))
    │
    └─► [3] 查询点赞状态
            │
            └─► 从Redis Set中查询 (O(1))


后台定时任务
    │
    ├─► [4] 从Redis待同步队列批量取出数据
    │       │
    │       ├─► 数据去重 (使用内存Set)
    │       │
    │       ├─► 批量写入MySQL
    │       │   ├─► INSERT ON DUPLICATE KEY UPDATE
    │       │   └─► 批量更新统计
    │       │
    │       └─► 清理Redis已同步数据
    │
    └─► [5] 定期同步Redis计数到MySQL
            │
            └─► 更新统计表中的点赞总数
```

---

## 3. 核心流程设计

### 3.1 点赞流程

```
┌─────────┐
│  用户   │
└────┬────┘
     │
     ▼
┌─────────────────────┐
│  发起点赞请求       │
│  POST /api/like     │
└────┬────────────────┘
     │
     ▼
┌─────────────────────┐
│  参数校验           │
│  - userId           │
│  - resourceId       │
│  - resourceType     │
└────┬────────────────┘
     │ 通过
     ▼
┌─────────────────────┐
│  检查是否已点赞     │
│  Redis: SISMEMBER   │
└────┬────────────────┘
     │ 未点赞
     ▼
┌─────────────────────────────┐
│  Redis Lua原子操作          │
│  1. SADD user:{userId}:{type}:{id} │
│  2. INCR count:{type}:{id}         │
│  3. LPUSH sync:queue               │
└────┬────────────────────────────┘
     │
     ▼
┌─────────────────────┐
│  发送MQ消息        │
│  异步通知其他服务   │
└────┬────────────────┘
     │
     ▼
┌─────────────────────┐
│  返回成功           │
│  {success: true}    │
└────┬────────────────┘
     │
     ▼
┌─────────┐
│  用户   │
└─────────┘
```

### 3.2 取消点赞流程

```
┌─────────┐
│  用户   │
└────┬────┘
     │
     ▼
┌─────────────────────────────┐
│  发起取消点赞请求            │
│  DELETE /api/like           │
└────┬────────────────────────┘
     │
     ▼
┌─────────────────────┐
│  参数校验           │
└────┬────────────────┘
     │ 通过
     ▼
┌─────────────────────┐
│  检查是否已点赞     │
│  Redis: SISMEMBER   │
└────┬────────────────┘
     │ 已点赞
     ▼
┌─────────────────────────────┐
│  Redis Lua原子操作          │
│  1. SREM user:{userId}:{type}:{id} │
│  2. DECR count:{type}:{id}         │
│  3. LPUSH sync:queue               │
└────┬────────────────────────────┘
     │
     ▼
┌─────────────────────┐
│  发送MQ消息        │
└────┬────────────────┘
     │
     ▼
┌─────────────────────┐
│  返回成功           │
└────┬────────────────┘
     │
     ▼
┌─────────┐
│  用户   │
└─────────┘
```

### 3.3 异步落库流程

```
定时任务触发 (每5秒)
    │
    ▼
┌─────────────────────────────┐
│  从Redis队列批量取数据      │
│  LRANGE sync:queue 0 -1000  │
└────┬────────────────────────┘
     │
     ▼
┌─────────────────────┐
│  数据解析与去重     │
│  使用内存Set去重    │
└────┬────────────────┘
     │
     ▼
┌─────────────────────────────┐
│  批量写入MySQL             │
│  INSERT ON DUPLICATE       │
│  KEY UPDATE                │
└────┬────────────────────────┘
     │
     ▼
┌─────────────────────┐
│  更新统计表         │
└────┬────────────────┘
     │
     ▼
┌─────────────────────┐
│  清理Redis已同步数据 │
└────┬────────────────┘
     │
     ▼
┌─────────────────────┐
│  记录同步日志       │
└────┬────────────────┘
     │
     ▼
结束
```

---

## 4. 技术实现方案

### 4.1 Redis数据结构设计

```redis
# 用户点赞状态 (Set)
Key: like:user:{userId}:{resourceType}
Value: Set<resourceId>
Expire: 7天
示例: like:user:1001:article = {2001, 2002, 2003}

# 资源点赞计数 (String)
Key: like:count:{resourceType}:{resourceId}
Value: Integer
Expire: 永不过期
示例: like:count:article:2001 = 156

# 待同步队列 (List)
Key: like:sync:queue
Value: JSON Array
Expire: 无
示例: like:sync:queue = [
  {"userId": 1001, "resourceType": "article", "resourceId": 2001, "action": "like"},
  {"userId": 1002, "resourceType": "article", "resourceId": 2001, "action": "like"},
  ...
]

# 资源点赞用户列表 (Set, 用于排行榜等场景)
Key: like:resource:{resourceType}:{resourceId}
Value: Set<userId>
Expire: 7天
示例: like:resource:article:2001 = {1001, 1002, 1003}
```

### 4.2 高并发处理策略（先sentinel做服务保护）

#### 4.2.1 流量削峰
```java
// 令牌桶限流算法
RateLimiter rateLimiter = RateLimiter.create(10000); // 每秒10000个请求

if (!rateLimiter.tryAcquire()) {
    throw new RateLimitException("请求过于频繁，请稍后再试");
}
```

#### 4.2.2 缓存预热
```java
@PostConstruct
public void init() {
    // 启动时预热热点资源数据
    preloadHotResources();
}

private void preloadHotResources() {
    // 从数据库读取热点数据
    List<LikeStatistics> hotStats = likeStatisticsMapper.selectHotResources(100);
    // 加载到Redis
    hotStats.forEach(stat -> {
        redisTemplate.opsForValue().set(
            "like:count:" + stat.getResourceType() + ":" + stat.getResourceId(),
            stat.getLikeCount()
        );
    });
}
```

#### 4.2.3 批量处理优化
```java
// 使用JDK 21虚拟线程处理高并发
ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

public CompletableFuture<Void> batchProcessLike(List<LikeRequest> requests) {
    List<CompletableFuture<Void>> futures = requests.stream()
        .map(request -> CompletableFuture.runAsync(() -> {
            processLike(request);
        }, virtualThreadExecutor))
        .toList();

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
}
```

### 4.3 MQ消息设计

```java
// 点赞事件消息
@Data
public class LikeEventMessage {
    private Long userId;
    private String resourceType; // article, video, comment
    private Long resourceId;
    private String action; // like, unlike
    private Long timestamp;
    private String traceId;
}

// RabbitMQ配置
@Configuration
public class RabbitMQConfig {
    
    @Bean
    public Queue likeQueue() {
        return new Queue("like.queue", true);
    }
    
    @Bean
    public TopicExchange likeExchange() {
        return new TopicExchange("like.exchange", true, false);
    }
    
    @Bean
    public Binding likeBinding() {
        return BindingBuilder.bind(likeQueue())
                .to(likeExchange())
                .with("like.event.*");
    }
}
```

---

## 5. 数据一致性保障

### 5.1 最终一致性策略

1. **强一致性查询**：先查Redis，Redis未命中时查数据库
2. **异步同步**：定时任务批量同步Redis数据到数据库
3. **补偿机制**：监控队列堆积，发现异常时触发补偿

### 5.2 数据一致性校验

```java
// 定时校验Redis和MySQL数据一致性
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
public void checkDataConsistency() {
    // 1. 随机抽取1000个资源
    // 2. 对比Redis和MySQL的点赞数
    // 3. 发现不一致时记录日志并告警
    // 4. 自动修正（可选）
}
```

### 5.3 异常处理

```java
// Redis故障降级策略
public LikeResponse likeWithFallback(LikeRequest request) {
    try {
        // 正常流程：先写Redis，异步写MySQL
        return likeService.like(request);
    } catch (RedisConnectionException e) {
        // Redis不可用：直接写MySQL
        log.error("Redis不可用，降级到MySQL", e);
        return likeService.likeToDB(request);
    }
}
```

---

## 6. 性能指标

### 6.1 性能目标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| QPS | 50,000+ | 支持每秒5万次点赞请求 |
| 响应时间 | < 20ms | 99%请求在20ms内完成 |
| 可用性 | 99.9% | 系统可用性目标 |
| 数据延迟 | < 5秒 | 最终一致性延迟 |

### 6.2 容量规划

| 组件 | 规格建议 | 并发数 |
|------|---------|--------|
| Redis | 主从+哨兵 | 10万QPS |
| MySQL | 主从+读写分离 | 5000 TPS |
| RabbitMQ | 集群 | 10万消息/秒 |

---

## 7. 监控与告警

### 7.1 监控指标

- Redis连接数、命中率、队列长度
- MySQL慢查询、连接数、TPS
- RabbitMQ消息堆积、消费速率
- API响应时间、错误率

### 7.2 告警规则

```yaml
alerts:
  - name: redis_sync_queue_too_long
    condition: sync_queue_length > 10000
    severity: warning
    
  - name: mysql_slow_query
    condition: slow_query_count > 100
    severity: warning
    
  - name: mq_consumer_lag
    condition: consumer_lag > 5000
    severity: critical
```

---

## 8. 安全设计

### 8.1 防刷策略

1. **IP限流**：单IP每分钟最多100次点赞
2. **用户限流**：单用户每分钟最多50次点赞
3. **频次限制**：同一用户对同一资源10秒内只能操作一次
4. **验证码**：异常行为触发验证码验证

### 8.2 权限控制

```java
// 点赞操作需要登录
@PreAuthorize("isAuthenticated()")
public LikeResponse like(LikeRequest request) {
    // 执行点赞逻辑
}
```

---

## 9. 扩展性设计（待定）

### 9.1 水平扩展

- 无状态服务，支持横向扩展
- Redis集群化部署
- MySQL读写分离+分库分表

### 9.2 功能扩展

- 支持更多资源类型（文章、视频、评论等）
- 点赞统计报表
- 点赞排行榜
- 点赞推荐算法
