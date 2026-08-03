package com.myblog.infrastructure.diagnostics;

import com.myblog.application.port.CacheDiagnostics;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisCacheDiagnostics implements CacheDiagnostics {

    private final StringRedisTemplate redis;

    public RedisCacheDiagnostics(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean available() {
        try {
            String reply = redis.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(reply);
        } catch (Exception ignored) {
            return false;
        }
    }
}
