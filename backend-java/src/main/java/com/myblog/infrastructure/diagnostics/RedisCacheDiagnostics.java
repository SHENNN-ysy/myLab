package com.myblog.infrastructure.diagnostics;

import com.myblog.application.port.CacheDiagnostics;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis 缓存健康诊断：通过 PING 命令探测缓存是否可用，供健康检查接口使用。
 */
@Repository
public class RedisCacheDiagnostics implements CacheDiagnostics {

    private final StringRedisTemplate redis; // Redis 操作入口，用于发送 PING

    public RedisCacheDiagnostics(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 探测 Redis 是否可用。
     *
     * @return 收到 PONG 应答返回 true，任何异常均视为不可用
     */
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
