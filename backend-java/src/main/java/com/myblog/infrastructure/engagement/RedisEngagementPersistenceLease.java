package com.myblog.infrastructure.engagement;

import com.myblog.application.port.EngagementPersistenceLease;
import com.myblog.common.constant.RedisKeyPrefix;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Redis 短租约实现：保证同一时刻只有一个实例执行互动绝对值落库批次。 */
@Component
public class RedisEngagementPersistenceLease implements EngagementPersistenceLease {
    public static final String LOCK_KEY = RedisKeyPrefix.BLOG + "lock:engagement-persistence";
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisEngagementPersistenceLease(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String tryAcquire(Duration lease) {
        String token = UUID.randomUUID().toString();
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(LOCK_KEY, token, lease)) ? token : null;
    }

    @Override
    public void release(String token) {
        redis.execute(RELEASE_SCRIPT, List.of(LOCK_KEY), token);
    }
}
