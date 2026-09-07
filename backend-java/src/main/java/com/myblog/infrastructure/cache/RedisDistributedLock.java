package com.myblog.infrastructure.cache;

import com.myblog.application.port.DistributedLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** Redis SET NX PX 分布式锁适配器，使用 token 比较删除防止误释放。 */
@Component
public class RedisDistributedLock implements DistributedLock {
    public static final String LOCK_PREFIX = "myblog:content:v1:lock:";
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisDistributedLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(String name, String token, Duration lease) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key(name), token, lease));
    }

    @Override
    public void release(String name, String token) {
        redis.execute(RELEASE_SCRIPT, List.of(key(name)), token);
    }

    private String key(String name) {
        return LOCK_PREFIX + name;
    }
}
