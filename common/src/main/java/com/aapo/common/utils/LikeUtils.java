package com.aapo.common.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.TypeReference;
import com.aapo.common.domain.LikeCountKey;
import com.aapo.common.domain.LikeOperate;
import com.aapo.common.domain.LikeUserKey;
import com.aapo.common.enums.ResourceType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@ConditionalOnClass(StringRedisTemplate.class)
@Component
@RequiredArgsConstructor
public class LikeUtils {
    private static final String LIKE_USER_KEY_FORMATE = "like:user:%s:%s";

    private static final String LIKE_COUNT_KEY_FORMATE = "like:count:%s:%s";

    private static final String LIKE_ASYNC_QUEUE_KEY = "like:async:queue";

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isExist(Long userId, ResourceType resourceType) {
        return stringRedisTemplate.hasKey(getLikeUserKey(userId, resourceType));
    }

    /**
     * 保存用户点赞记录
     * @param userId
     * @param resourceType
     * @param resourceIds
     */
    public void saveLikeUserValue(Long userId, ResourceType resourceType, Collection<Long> resourceIds) {
        if (CollUtils.isEmpty(resourceIds)) {
            resourceIds = Collections.emptySet();
        }
        resourceIds.add(-1L);//添加默认值
        String[] values = resourceIds.stream().map(Object::toString).distinct().toArray(String[]::new);
        stringRedisTemplate.opsForSet().add(getLikeUserKey(userId, resourceType), values);
    }


    /**
     * 批量获取用户点赞记录
     * @param likeUserKeys
     * @return
     */
    public Map<String, Set<Long>> batchGetLikeUserValue(Collection<String> likeUserKeys) {
        List<String> list = new ArrayList<>(likeUserKeys);
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        //lua脚本
        String scriptText = """
                 local result = {}
                 for i, key in ipairs(KEYS) do
                     local members = redis.call('SMEMBERS', key)
                     local filtered = {}
                     -- 过滤掉 -1
                     for _, val in ipairs(members) do
                         if val ~= '-1' then
                             table.insert(filtered, val)
                         end
                     end
                     result[i] = filtered
                 end
                 return result
                """;
        redisScript.setScriptText(scriptText);
        redisScript.setResultType(List.class);
        Map<String, Set<Long>> newResult = new HashMap<>();
        List<List<String>> result = stringRedisTemplate.execute(redisScript, list);
        for (int i = 0; i < list.size(); i++) {
            newResult.put(list.get(i), result.get(i).stream().map(Long::parseLong).collect(Collectors.toSet()));
        }
        return newResult;
    }



    public void saveLikeCountValue(ResourceType resourceType, Long resourceId, String count) {
        stringRedisTemplate.opsForValue().set(getLikeCountKey(resourceType, resourceId), count);
    }

    /**
     * 批量获取点赞数数据
     * @param likeCountKeys
     * @return
     */
    public Map<String, Integer> batchGetLikeCountValue(Collection<String> likeCountKeys) {
        List<String> list = new ArrayList<>(likeCountKeys);
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        //lua脚本
        String scriptText = """
                 local result = {}
                 for i, key in ipairs(KEYS) do
                     local value = redis.call('GET', key)
                     result[i] = value
                 end
                 return result
                """;
        redisScript.setScriptText(scriptText);
        redisScript.setResultType(List.class);
        Map<String, Integer> newResult = new HashMap<>();
        List<String> result = stringRedisTemplate.execute(redisScript, list);
        for (int i = 0; i < list.size(); i++) {
            newResult.put(list.get(i), Integer.parseInt(result.get(i)));
        }
        return newResult;
    }

    /**
     * 点赞
     *
     * @param userId
     * @param resourceType
     * @param resourceId
     */
    public void like(Long userId, ResourceType resourceType, Long resourceId) {
        LikeOperate likeOperate = new LikeOperate(userId, resourceType, resourceId, LikeOperate.Action.like);
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        //lua脚本
        String scriptText = """
                -- 判断重复点赞
                local isLiked = redis.call("SISMEMBER", KEYS[1], ARGV[1])
                if tonumber(isLiked) == 1 then
                    return 0
                end
                
                -- 将资源ID加入用户点赞集合
                redis.call("SADD", KEYS[1], ARGV[1])
                -- 资源总点赞数 +1
                local newLikeCount = redis.call("INCR", KEYS[2])
                -- 点赞操作写入待同步队列
                redis.call("LPUSH", KEYS[3], ARGV[2])
                
                -- 返回最新的点赞总数
                return newLikeCount
                """;
        redisScript.setScriptText(scriptText);
        redisScript.setResultType(Long.class);
        try {
            stringRedisTemplate.execute(redisScript,
                    Arrays.asList(getLikeUserKey(userId, resourceType), getLikeCountKey(resourceType, resourceId), LIKE_ASYNC_QUEUE_KEY),
                    resourceId.toString(), objectMapper.writeValueAsString(likeOperate));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 取消点赞
     *
     * @param userId
     * @param resourceType
     * @param resourceId
     */
    public void unlike(Long userId, ResourceType resourceType, Long resourceId) {
        LikeOperate likeOperate = new LikeOperate(userId, resourceType, resourceId, LikeOperate.Action.unlike);
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        //lua脚本
        String scriptText = """
                -- 判断未点赞
                local isLiked = redis.call("SISMEMBER", KEYS[1], ARGV[1])
                if tonumber(isLiked) == 0 then
                    return 0
                end
                
                -- 将资源ID加入用户点赞集合
                redis.call("SREM", KEYS[1], ARGV[1])
                -- 资源总点赞数 -1
                local newLikeCount = redis.call("DECR", KEYS[2])
                -- 点赞操作写入待同步队列
                redis.call("LPUSH", KEYS[3], ARGV[2])
                
                -- 返回最新的点赞总数
                return newLikeCount
                """;
        redisScript.setScriptText(scriptText);
        redisScript.setResultType(Long.class);
        try {
            stringRedisTemplate.execute(redisScript,
                    Arrays.asList(getLikeUserKey(userId, resourceType), getLikeCountKey(resourceType, resourceId), LIKE_ASYNC_QUEUE_KEY),
                    resourceId.toString(), objectMapper.writeValueAsString(likeOperate));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取点赞操作
     * @return
     */
    public List<LikeOperate> getLikeOperate(int count) {
        List<String> list = stringRedisTemplate.opsForList().range(LIKE_ASYNC_QUEUE_KEY, 0, count);//测试用
        //List<String> list = stringRedisTemplate.opsForList().leftPop(LIKE_ASYNC_QUEUE_KEY, count);
        if (CollUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().distinct().map(item -> {
            try {
                return objectMapper.readValue(item, LikeOperate.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }



    public String getLikeUserKey(Long userId, ResourceType resourceType) {
        return String.format(LIKE_USER_KEY_FORMATE, userId, resourceType);
    }
    public LikeUserKey getLikeUserKey(String key) {
        String[] split = key.split(":");
        return new LikeUserKey(Long.parseLong(split[2]), ResourceType.valueOf(split[3]));
    }

    public LikeCountKey getLikeCountKey(String key) {
        String[] split = key.split(":");
        return new LikeCountKey(ResourceType.valueOf(split[2]), Long.parseLong(split[3]));
    }

    public String getLikeCountKey(ResourceType resourceType, Long resourceId) {
        return String.format(LIKE_COUNT_KEY_FORMATE, resourceType, resourceId);
    }
}