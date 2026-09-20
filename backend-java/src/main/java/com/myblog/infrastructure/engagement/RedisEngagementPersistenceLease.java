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
    /** 解锁脚本：仅当锁值与自己持有的 token 一致才删除，避免误删租约过期后被他人重新获取的锁。 */
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

    /**
     * 以 SET NX + TTL 争抢唯一锁键实现单活租约。
     *
     * @param lease 租约有效期；持有者宕机时由 TTL 兜底自动释放
     * @return 获取成功返回本次唯一 token（释放时校验用），已有他人持有则返回 null
     */
    @Override
    public String tryAcquire(Duration lease) {
        String token = UUID.randomUUID().toString();
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(LOCK_KEY, token, lease)) ? token : null;
    }

    /** 按 token 原子校验后释放租约（见 RELEASE_SCRIPT）；token 不匹配时静默不删。 */
    @Override
    public void release(String token) {
        redis.execute(RELEASE_SCRIPT, List.of(LOCK_KEY), token);
    }
}
