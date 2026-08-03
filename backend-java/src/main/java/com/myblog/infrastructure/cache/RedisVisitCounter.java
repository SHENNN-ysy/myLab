package com.myblog.infrastructure.cache;

import com.myblog.application.port.VisitCounter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisVisitCounter implements VisitCounter {

    private final StringRedisTemplate redis;

    public RedisVisitCounter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void record(String day, String visitorId) {
        redis.opsForValue().increment(key(day, "pv"));
        redis.opsForHyperLogLog().add(key(day, "uv"), visitorId);
    }

    @Override
    public long pageViews(String day) {
        String value = redis.opsForValue().get(key(day, "pv"));
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    @Override
    public long uniqueVisitors(String day) {
        Long value = redis.opsForHyperLogLog().size(key(day, "uv"));
        return value == null ? 0L : value;
    }

    private static String key(String day, String metric) {
        return "stats:visit:" + day + ":" + metric;
    }
}
