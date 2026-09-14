package com.myblog.infrastructure.cache;

import com.myblog.application.port.PublishedPostCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

/**
 * 已发布 MyLab 文章索引的 Redis 实现：单个 Set 承载全部已发布 post_key。
 * 重建走临时集合 + RENAME 原子替换，中途失败不会留下半新半旧的索引；
 * 索引带 24 小时 TTL 兜底，重建失败残留的过期数据到期后请求自动回源数据库并补写。
 */
@Slf4j
@Component
public class RedisPublishedPostCache implements PublishedPostCache {
    static final String INDEX_KEY = "blog:engagement:published-posts";
    private static final Duration INDEX_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public RedisPublishedPostCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean contains(String postKey) {
        try {
            return Boolean.TRUE.equals(redis.opsForSet().isMember(INDEX_KEY, postKey));
        } catch (RuntimeException exception) {
            // Redis 故障按未命中处理，由调用方回源数据库，不影响互动接口可用性
            log.debug("已发布文章索引读取失败，按未命中处理：error={}", exception.toString());
            return false;
        }
    }

    @Override
    public void add(String postKey) {
        try {
            redis.opsForSet().add(INDEX_KEY, postKey);
            redis.expire(INDEX_KEY, INDEX_TTL);
        } catch (RuntimeException exception) {
            log.debug("已发布文章索引补写失败：postKey={}, error={}", postKey, exception.toString());
        }
    }

    @Override
    public void rebuild(Collection<String> postKeys) {
        if (postKeys.isEmpty()) {
            // 当前无已发布文章：直接清空索引，后续请求回源数据库兜底
            try {
                redis.delete(INDEX_KEY);
            } catch (RuntimeException exception) {
                log.warn("已发布文章索引清空失败：error={}", exception.toString());
            }
            return;
        }
        String stagingKey = INDEX_KEY + ":staging:" + UUID.randomUUID();
        try {
            redis.opsForSet().add(stagingKey, postKeys.toArray(new String[0]));
            // RENAME 原子覆盖旧索引，替换完成前读到的仍是旧集合
            redis.rename(stagingKey, INDEX_KEY);
            redis.expire(INDEX_KEY, INDEX_TTL);
        } catch (RuntimeException exception) {
            log.warn("已发布文章索引重建失败：size={}, error={}", postKeys.size(), exception.toString());
            try {
                redis.delete(stagingKey);
            } catch (RuntimeException cleanupError) {
                log.debug("已发布文章索引临时集合清理失败：error={}", cleanupError.toString());
            }
        }
    }
}
