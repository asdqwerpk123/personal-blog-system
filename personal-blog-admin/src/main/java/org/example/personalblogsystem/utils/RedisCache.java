package org.example.personalblogsystem.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存工具类，封装登录态等简单键值数据的读写删除操作。
 * 当前主要服务于 JWT 登录后的 LoginUser 服务端缓存。
 */
@Component
public class RedisCache {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 写入带过期时间的缓存对象。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间数值
     * @param timeUnit 过期时间单位
     * @param <T> 缓存值类型
     */
    public <T> void setCacheObject(String key, T value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 按键读取缓存对象。
     *
     * @param key 缓存键
     * @param <T> 期望返回类型
     * @return 缓存对象；不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getCacheObject(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除指定缓存键。
     *
     * @param key 缓存键
     * @return 删除成功返回 true，否则返回 false
     */
    public boolean deleteObject(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }
}
